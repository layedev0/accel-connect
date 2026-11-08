import { Component, inject, OnInit, signal } from '@angular/core';
import { PlatService } from '../../services/impl/plat.service';
import { Meal } from '../../model/plat.model';
import { RouterLink } from '@angular/router';
import { NotificationService } from '../../../../core/services/impl/notification.service';
import { ConfirmationService } from '../../../../core/services/impl/ConfirmationDialog.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Ajout de FormsModule pour le ngModel de la recherche
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs'; // Ajout pour optimiser la recherche

@Component({
  selector: 'app-list-plat-component',
  // Ajout de FormsModule
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './list-plat-component.html',
  styleUrl: './list-plat-component.css',
})
export class ListPlatComponent implements OnInit {
  private readonly platService = inject(PlatService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);

  // Liste complète des plats chargés depuis le service
  allPlats = signal<Meal[]>([]);
  // Liste des plats affichés après filtrage
  filteredPlats = signal<Meal[]>([]);
  totalPlats = signal<number>(0);
  isEditModalOpen = signal(false);
  selectedPlat = signal<Meal | null>(null);
  isSubmitting = signal(false);

  // Champ de recherche lié à l'input
  searchTerm: string = '';
  // Subject pour gérer le debounce de la recherche
  private searchTerms = new Subject<string>();

  ngOnInit(): void {
    this.loadPlats();

    // Souscription pour gérer la recherche avec debounce (pour ne pas filtrer à chaque frappe)
    this.searchTerms
      .pipe(
        // Attend 300ms après la dernière frappe
        debounceTime(300),
        // N'émet que si la valeur a changé
        distinctUntilChanged()
      )
      .subscribe((term) => {
        this.filterPlats(term);
      });
  }

  /**
   * Normalise une chaîne pour la recherche :
   * 1. Supprime les accents (é -> e, à -> a, etc.)
   * 2. Convertit en minuscules
   * 3. Remplace les ligatures (comme 'œ' par 'oe')
   * @param text La chaîne à normaliser.
   * @returns La chaîne normalisée.
   */
  private normalizeSearch(text: string): string {
    if (!text) return '';

    // 1. Convertir en minuscule
    let normalized = text.toLowerCase();

    // 2. Remplacer les ligatures courantes (œ -> oe, æ -> ae)
    normalized = normalized.replace(/œ/g, 'oe').replace(/æ/g, 'ae');

    // 3. Supprimer les accents (méthode standard en JS)
    // NFD (Normalization Form D) décompose les caractères accentués en caractère de base + marque d'accent
    // regex /\p{Diacritic}/gu supprime toutes les marques d'accent
    normalized = normalized.normalize('NFD').replace(/[\u0300-\u036f]/g, '');

    return normalized;
  }

  loadPlats(): void {
    this.platService.getPlats().subscribe({
      next: (data) => {
        this.allPlats.set(data);
        // Au chargement initial, les plats filtrés sont tous les plats
        this.filterPlats(this.searchTerm);
      },
      error: (error) => {
        console.error(error);
        this.notificationService.error('Erreur de chargement', 'Impossible de charger les plats');
      },
    });
  }

  /**
   * Déclenche le filtrage basé sur le terme de recherche actuel.
   * Cette méthode est appelée lors de l'initialisation et à chaque nouvelle valeur du searchTerms Subject.
   */
  filterPlats(term: string): void {
    const normalizedTerm = this.normalizeSearch(term);

    const results = this.allPlats().filter((plat) => {
      // Normaliser le nom du plat pour la comparaison
      const normalizedPlatName = this.normalizeSearch(plat.mealName || '');

      // La recherche est effectuée sans tenir compte des accents ni des ligatures
      return normalizedPlatName.includes(normalizedTerm);
    });

    this.filteredPlats.set(results);
    this.totalPlats.set(results.length);
  }

  /**
   * Gère l'événement d'input sur le champ de recherche.
   * Utilise le Subject pour appliquer un debounce.
   */
  onSearchChange(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.searchTerm = term;
    this.searchTerms.next(term);
  }

  openEditModal(plat: Meal): void {
    this.selectedPlat.set({ ...plat });
    this.isEditModalOpen.set(true);
  }

  closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedPlat.set(null);
    this.isSubmitting.set(false);
  }

  updatePlat(event: Event): void {
    event.preventDefault();
    const plat = this.selectedPlat();

    if (!plat || !plat.id) return;

    // Validation
    if (!plat.mealName?.trim()) {
      this.notificationService.warning('Champ requis', 'Le nom du plat est obligatoire');
      return;
    }

    if (!plat.unitPrice || plat.unitPrice <= 0) {
      this.notificationService.warning('Prix invalide', 'Le prix doit être supérieur à 0');
      return;
    }

    this.isSubmitting.set(true);

    this.platService.updatePlat(plat.id, plat).subscribe({
      next: (updated) => {
        const currentPlats = this.allPlats();
        const index = currentPlats.findIndex((p) => p.id === updated.id);

        if (index !== -1) {
          currentPlats[index] = updated;
          this.allPlats.set([...currentPlats]); // Mettre à jour la liste complète
          this.filterPlats(this.searchTerm); // Refiltrer pour mettre à jour l'affichage
        }

        this.notificationService.success(
          'Plat modifié',
          `${updated.mealName} a été mis à jour avec succès`
        );
        this.closeEditModal();
      },
      error: (error) => {
        console.error('Erreur mise à jour:', error);
        this.isSubmitting.set(false);
        this.notificationService.error('Erreur de modification', 'Impossible de modifier le plat');
      },
    });
  }

  deletePlat(plat: Meal): void {
    this.confirmationService.confirmDelete(
      'Supprimer ce plat ?',
      `Êtes-vous sûr de vouloir supprimer "${plat.mealName}" ? Cette action est irréversible.`,
      () => {
        if (!plat.id) return;

        this.platService.deletePlat(plat.id).subscribe({
          next: () => {
            const currentPlats = this.allPlats().filter((p) => p.id !== plat.id);
            this.allPlats.set(currentPlats); // Mettre à jour la liste complète
            this.filterPlats(this.searchTerm); // Refiltrer pour mettre à jour l'affichage

            this.notificationService.success(
              'Plat supprimé',
              `${plat.mealName} a été supprimé avec succès`
            );
          },
          error: (error) => {
            console.error('Erreur suppression:', error);
            this.notificationService.error(
              'Erreur de suppression',
              'Impossible de supprimer le plat'
            );
          },
        });
      }
    );
  }

  updateField(field: keyof Meal, value: any): void {
    const plat = this.selectedPlat();
    if (plat) {
      this.selectedPlat.set({ ...plat, [field]: value });
    }
  }
}
