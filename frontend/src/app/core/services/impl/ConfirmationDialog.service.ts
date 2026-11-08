import { Injectable, signal } from '@angular/core';
import { ConfirmationDialog } from '../../models/dialog.model';

@Injectable({
  providedIn: 'root',
})
export class ConfirmationService {
  isOpen = signal(false);
  currentDialog = signal<ConfirmationDialog | null>(null);

  /**
   * Affiche une boîte de confirmation
   */
  confirm(dialog: ConfirmationDialog): void {
    this.currentDialog.set({
      confirmText: 'Confirmer',
      cancelText: 'Annuler',
      type: 'info',
      ...dialog,
    });
    this.isOpen.set(true);
  }

  /**
   * Confirmation de suppression
   */
  confirmDelete(
    title: string,
    message: string,
    onConfirm: () => void,
    onCancel?: () => void
  ): void {
    this.confirm({
      title,
      message,
      type: 'danger',
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      onConfirm,
      onCancel,
    });
  }

  /**
   * Confirmation d'annulation
   */
  confirmCancel(
    title: string,
    message: string,
    onConfirm: () => void,
    onCancel?: () => void
  ): void {
    this.confirm({
      title,
      message,
      type: 'warning',
      confirmText: 'Annuler la commande',
      cancelText: 'Retour',
      onConfirm,
      onCancel,
    });
  }

  /**
   * Confirmation d'action
   */
  confirmAction(
    title: string,
    message: string,
    confirmText: string,
    onConfirm: () => void,
    onCancel?: () => void
  ): void {
    this.confirm({
      title,
      message,
      type: 'info',
      confirmText,
      cancelText: 'Annuler',
      onConfirm,
      onCancel,
    });
  }

  /**
   * Ferme la boîte de dialogue
   */
  close(): void {
    this.isOpen.set(false);
    setTimeout(() => {
      this.currentDialog.set(null);
    }, 300);
  }

  /**
   * Gère la confirmation
   */
  handleConfirm(): void {
    const dialog = this.currentDialog();
    if (dialog) {
      dialog.onConfirm();
    }
    this.close();
  }

  /**
   * Gère l'annulation
   */
  handleCancel(): void {
    const dialog = this.currentDialog();
    if (dialog?.onCancel) {
      dialog.onCancel();
    }
    this.close();
  }
}
