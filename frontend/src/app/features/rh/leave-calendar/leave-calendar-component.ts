import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { forkJoin } from 'rxjs';
import { LeaveService, LeaveRequestSummary } from '../services/leave.service';
import { AbsenceService, AbsenceSummary } from '../services/absence.service';
import { HolidayService, Holiday } from '../services/holiday.service';
import { ClickOutsideDirective } from '../../../shared/directives/clickOutside.directive';

interface CalendarDay {
  date: Date;
  day: number;
  iso: string;
  inMonth: boolean;
  isWeekend: boolean;
  isToday: boolean;
  holiday: Holiday | null;
  absences: AbsenceSummary[];
}

interface LeaveBar {
  leave: LeaveRequestSummary;
  leftStyle: string;
  widthStyle: string;
  top: number;
  lane: number;
  continuesLeft: boolean;
  continuesRight: boolean;
  isEnd: boolean;
  color: string;
  initials: string;
  label: string;
}

interface CalendarWeek {
  days: CalendarDay[];
  bars: LeaveBar[];
  laneCount: number;
}

interface LegendItem {
  label: string;
  color: string;
}

interface Popover {
  x: number;
  y: number;
  title: string;
  typeLabel: string;
  dateLabel: string;
  status: string;
  color: string;
}

const LANE_HEIGHT = 26;

@Component({
  selector: 'app-leave-calendar',
  standalone: true,
  imports: [ClickOutsideDirective],
  templateUrl: './leave-calendar-component.html',
  styleUrl: './leave-calendar-component.css',
})
export class LeaveCalendarComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly absenceService = inject(AbsenceService);
  private readonly holidayService = inject(HolidayService);

  readonly loading = signal(true);
  readonly leaves = signal<LeaveRequestSummary[]>([]);
  readonly absences = signal<AbsenceSummary[]>([]);
  readonly holidays = signal<Holiday[]>([]);
  readonly popover = signal<Popover | null>(null);

  /** First day of the displayed month (defaults to the current month). */
  readonly currentMonth = signal<Date>(this.startOfMonth(new Date()));

  readonly weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  readonly laneHeight = LANE_HEIGHT;

  private readonly typeColors: Record<string, string> = {
    ANNUAL_LEAVE: '#25509d',
    RECUPERATION: '#0d9488',
    SICK_LEAVE: '#b45309',
  };
  private readonly defaultColor = '#64748b';

  private readonly holidaysByDate = computed(() => {
    const map = new Map<string, Holiday>();
    for (const h of this.holidays()) map.set(h.date, h);
    return map;
  });

  private readonly absencesByDate = computed(() => {
    const map = new Map<string, AbsenceSummary[]>();
    for (const a of this.absences()) {
      const list = map.get(a.date) ?? [];
      list.push(a);
      map.set(a.date, list);
    }
    return map;
  });

  readonly monthLabel = computed(() => {
    const label = this.currentMonth().toLocaleDateString('fr-FR', {
      month: 'long',
      year: 'numeric',
    });
    return label.charAt(0).toUpperCase() + label.slice(1);
  });

  readonly legendItems = computed<LegendItem[]>(() => {
    const seen = new Map<string, LegendItem>();
    for (const l of this.leaves()) {
      if (!seen.has(l.leaveTypeCode)) {
        seen.set(l.leaveTypeCode, {
          label: l.leaveTypeName || l.leaveTypeCode,
          color: this.leaveColor(l.leaveTypeCode),
        });
      }
    }
    return [...seen.values()];
  });

  readonly weeks = computed<CalendarWeek[]>(() => {
    const monthStart = this.currentMonth();
    const gridStart = this.addDays(monthStart, -this.mondayIndex(monthStart));
    const month = monthStart.getMonth();
    const todayIso = this.toIso(new Date());
    const holidays = this.holidaysByDate();
    const absences = this.absencesByDate();

    const weeks: CalendarWeek[] = [];
    for (let w = 0; w < 6; w++) {
      const weekStart = this.addDays(gridStart, w * 7);
      const days: CalendarDay[] = [];
      for (let d = 0; d < 7; d++) {
        const date = this.addDays(weekStart, d);
        const iso = this.toIso(date);
        const dow = date.getDay();
        days.push({
          date,
          day: date.getDate(),
          iso,
          inMonth: date.getMonth() === month,
          isWeekend: dow === 0 || dow === 6,
          isToday: iso === todayIso,
          holiday: holidays.get(iso) ?? null,
          absences: absences.get(iso) ?? [],
        });
      }
      const bars = this.buildBars(weekStart, this.addDays(weekStart, 6));
      const laneCount = bars.reduce((max, b) => Math.max(max, b.lane + 1), 0);
      weeks.push({ days, bars, laneCount });
    }
    return weeks;
  });

  ngOnInit(): void {
    forkJoin({
      leaves: this.leaveService.getApprovedRequests(),
      absences: this.absenceService.getApprovedAbsences(),
    }).subscribe({
      next: ({ leaves, absences }) => {
        this.leaves.set(leaves);
        this.absences.set(absences);
        this.loading.set(false);
        this.loadHolidays();
      },
      error: () => this.loading.set(false),
    });
  }

  // ---- Month navigation ----

  previousMonth(): void {
    this.shiftMonth(-1);
  }

  nextMonth(): void {
    this.shiftMonth(1);
  }

  goToToday(): void {
    this.popover.set(null);
    this.currentMonth.set(this.startOfMonth(new Date()));
    this.loadHolidays();
  }

  private shiftMonth(delta: number): void {
    this.popover.set(null);
    const cur = this.currentMonth();
    this.currentMonth.set(new Date(cur.getFullYear(), cur.getMonth() + delta, 1));
    this.loadHolidays();
  }

  private loadHolidays(): void {
    const monthStart = this.currentMonth();
    const gridStart = this.addDays(monthStart, -this.mondayIndex(monthStart));
    const gridEnd = this.addDays(gridStart, 41);
    this.holidayService.getHolidays(this.toIso(gridStart), this.toIso(gridEnd)).subscribe({
      next: (holidays) => this.holidays.set(holidays),
      error: () => this.holidays.set([]),
    });
  }

  // ---- Leave bar layout ----

  private buildBars(weekStart: Date, weekEnd: Date): LeaveBar[] {
    const segments = this.leaves()
      .map((leave) => this.toSegment(leave, weekStart, weekEnd))
      .filter((s): s is LeaveBar => s !== null)
      .sort((a, b) => this.startColOf(a) - this.startColOf(b));

    // Greedy lane assignment: place each bar in the lowest lane that is free.
    const laneEnds: number[] = [];
    for (const seg of segments) {
      const start = this.startColOf(seg);
      const end = this.endColOf(seg);
      let lane = 0;
      while (lane < laneEnds.length && laneEnds[lane] >= start) lane++;
      laneEnds[lane] = end;
      seg.lane = lane;
      seg.top = lane * LANE_HEIGHT;
    }
    return segments;
  }

  // Temp storage of columns on the bar object during layout.
  private readonly colCache = new WeakMap<LeaveBar, [number, number]>();
  private startColOf(bar: LeaveBar): number {
    return this.colCache.get(bar)![0];
  }
  private endColOf(bar: LeaveBar): number {
    return this.colCache.get(bar)![1];
  }

  private toSegment(leave: LeaveRequestSummary, weekStart: Date, weekEnd: Date): LeaveBar | null {
    const leaveStart = this.parseIso(leave.startDate);
    const leaveEnd = this.parseIso(leave.endDate);
    if (leaveEnd < weekStart || leaveStart > weekEnd) return null;

    const segStart = leaveStart < weekStart ? weekStart : leaveStart;
    const segEnd = leaveEnd > weekEnd ? weekEnd : leaveEnd;
    const startCol = this.diffDays(segStart, weekStart);
    const endCol = this.diffDays(segEnd, weekStart);
    const span = endCol - startCol + 1;

    const bar: LeaveBar = {
      leave,
      leftStyle: `calc(${((startCol / 7) * 100).toFixed(4)}% + 3px)`,
      widthStyle: `calc(${((span / 7) * 100).toFixed(4)}% - 6px)`,
      top: 0,
      lane: 0,
      continuesLeft: leaveStart < weekStart,
      continuesRight: leaveEnd > weekEnd,
      isEnd: this.isSameDay(segEnd, leaveEnd),
      color: this.leaveColor(leave.leaveTypeCode),
      initials: this.initials(leave.employeeName),
      label: leave.employeeName || 'Employé',
    };
    this.colCache.set(bar, [startCol, endCol]);
    return bar;
  }

  private leaveColor(code: string): string {
    return this.typeColors[code] ?? this.defaultColor;
  }

  // ---- Popover ----

  openLeave(bar: LeaveBar, event: MouseEvent): void {
    event.stopPropagation();
    const l = bar.leave;
    const dateLabel =
      l.startDate === l.endDate
        ? this.formatFull(l.startDate)
        : `${this.formatFull(l.startDate)} – ${this.formatFull(l.endDate)}`;
    this.popover.set({
      ...this.anchor(event),
      title: l.employeeName || 'Employé',
      typeLabel: l.leaveTypeName || l.leaveTypeCode,
      dateLabel: `${dateLabel} · ${l.totalDays} j`,
      status: l.overallStatus,
      color: bar.color,
    });
  }

  openAbsence(absence: AbsenceSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.popover.set({
      ...this.anchor(event),
      title: absence.employeeName || 'Employé',
      typeLabel: absence.absenceTypeName || 'Absence',
      dateLabel: `${this.formatFull(absence.date)} · ${this.time(absence.startTime)}–${this.time(absence.endTime)}`,
      status: absence.status,
      color: this.defaultColor,
    });
  }

  closePopover(): void {
    this.popover.set(null);
  }

  private anchor(event: MouseEvent): { x: number; y: number } {
    const width = 260;
    const x = Math.min(event.clientX + 12, window.innerWidth - width - 12);
    const y = Math.min(event.clientY + 12, window.innerHeight - 160);
    return { x: Math.max(12, x), y: Math.max(12, y) };
  }

  statusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-amber-100 text-amber-800';
      case 'Approuvé':   return 'bg-blue-100 text-blue-800';
      case 'Pris':       return 'bg-green-100 text-green-800';
      case 'Rejeté':     return 'bg-red-100 text-red-800';
      case 'Annulé':     return 'bg-gray-100 text-gray-500';
      default:           return 'bg-green-100 text-green-800';
    }
  }

  time(t: string | undefined): string {
    return t ? t.slice(0, 5) : '';
  }

  // ---- Date helpers ----

  private startOfMonth(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  /** 0 for Monday … 6 for Sunday. */
  private mondayIndex(d: Date): number {
    return (d.getDay() + 6) % 7;
  }

  private addDays(d: Date, n: number): Date {
    const r = new Date(d);
    r.setDate(r.getDate() + n);
    return r;
  }

  private diffDays(a: Date, b: Date): number {
    return Math.round((a.getTime() - b.getTime()) / 86_400_000);
  }

  private isSameDay(a: Date, b: Date): boolean {
    return this.toIso(a) === this.toIso(b);
  }

  private parseIso(s: string): Date {
    return new Date(s + 'T00:00:00');
  }

  private toIso(d: Date): string {
    return [
      d.getFullYear(),
      String(d.getMonth() + 1).padStart(2, '0'),
      String(d.getDate()).padStart(2, '0'),
    ].join('-');
  }

  private formatFull(s: string): string {
    return this.parseIso(s).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  private initials(name: string | undefined): string {
    if (!name) return '?';
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');
  }
}
