import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Classe de validateurs personnalisés réutilisables
 */
export class CustomValidators {
  /**
   * Validateur pour interdire les chiffres dans un champ texte
   * Utile pour les noms, prénoms, etc.
   */
  static noNumber(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const hasNumber = /\d/.test(control.value);
    return hasNumber ? { hasNumber: true } : null;
  }

  /**
   * Validateur pour vérifier qu'une valeur est un entier
   * Accepte les strings contenant uniquement des chiffres
   */
  static integer(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const value = control.value.toString().trim();
    const isInteger = /^\d+$/.test(value);

    return isInteger ? null : { notInteger: true };
  }

  /**
   * Validateur pour vérifier une valeur minimale
   * @param min - La valeur minimale requise
   */
  static minValue(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const value = Number.parseInt(control.value, 10);
      return !Number.isNaN(value) && value >= min ? null : { minValue: { min, actual: value } };
    };
  }

  /**
   * Validateur pour vérifier une valeur maximale
   * @param max - La valeur maximale requise
   */
  static maxValue(max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      const value = Number.parseInt(control.value, 10);
      return !Number.isNaN(value) && value <= max ? null : { maxValue: { max, actual: value } };
    };
  }

  /**
   * Validateur pour vérifier qu'un champ contient uniquement des lettres
   * Accepte les lettres accentuées et les espaces
   */
  static onlyLetters(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const isLettersOnly = /^[a-zA-ZÀ-ÿ\s]+$/.test(control.value);
    return isLettersOnly ? null : { onlyLetters: true };
  }

  /**
   * Validateur pour vérifier un numéro de téléphone sénégalais
   */
  static phoneNumberSN(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const phonePattern = /^(77|78|76|70|75)\d{7}$/;
    const isValid = phonePattern.test(control.value.toString().replaceAll(/\s/g, ''));
    return isValid ? null : { invalidPhone: true };
  }

  /**
   * Validateur pour vérifier qu'un string ne contient pas d'espaces
   */
  static noWhitespace(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const hasWhitespace = /\s/.test(control.value);
    return hasWhitespace ? { hasWhitespace: true } : null;
  }

  /**
   * Validateur pour vérifier le format d'un email personnalisé
   */
  static customEmail(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;

    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const isValid = emailPattern.test(control.value);

    return isValid ? null : { invalidEmail: true };
  }
}
