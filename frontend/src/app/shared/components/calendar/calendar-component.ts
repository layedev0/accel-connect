import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-calendar-component',
  imports: [CommonModule],
  templateUrl: './calendar-component.html',
  styleUrl: './calendar-component.css',
})
export class PrettyCalendarComponent {
  today = new Date();
  currentMonth = this.today.getMonth();
  currentYear = this.today.getFullYear();

  get monthName(): string {
    return this.today.toLocaleString('fr-FR', { month: 'long', year: 'numeric' });
  }

  get days(): number[] {
    const daysInMonth = new Date(this.currentYear, this.currentMonth + 1, 0).getDate();
    return Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  isToday(day: number): boolean {
    return (
      this.today.getDate() === day &&
      this.today.getMonth() === this.currentMonth &&
      this.today.getFullYear() === this.currentYear
    );
  }
}
