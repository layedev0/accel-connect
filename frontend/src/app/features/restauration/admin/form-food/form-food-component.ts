import { Component, inject, OnInit } from '@angular/core';
import { PlatService } from '../../services/impl/plat.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CustomValidators } from '../../../../core/validators/CustomValidators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-form-food-component',
  imports: [ReactiveFormsModule, RouterLink, CommonModule, FontAwesomeModule],
  templateUrl: './form-food-component.html',
  styleUrl: './form-food-component.css',
})
export class FormFoodComponent implements OnInit {
  private readonly platService = inject(PlatService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  platForm!: FormGroup;
  isSubmitting = false;

  ngOnInit(): void {
    this.platForm = this.fb.group({
      mealName: ['', [Validators.required, Validators.minLength(3), CustomValidators.noNumber]],
      unitPrice: [
        '',
        [Validators.required, CustomValidators.integer, CustomValidators.minValue(300)],
      ],
      imageUrl: [''],
    });
  }

  onSubmit(): void {
    if (this.platForm.invalid) {
      this.platForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const formValue = {
      ...this.platForm.value,
      unitPrice: Number.parseInt(this.platForm.value.unitPrice, 10),
    };

    this.platService.createPlats(formValue).subscribe({
      next: () => {
        this.platForm.reset();
        this.router.navigate(['/admin/liste-food']);
      },
      error: (err) => {
        this.isSubmitting = false;
      },
      complete: () => (this.isSubmitting = false),
    });
  }

  hasError(fieldName: string): boolean {
    const field = this.platForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getError(fieldName: string): string {
    const field = this.platForm.get(fieldName);
    if (!field) return '';

    if (field.hasError('required')) return 'Ce champ est requis';
    if (field.hasError('minlength'))
      return `Minimum ${field.errors?.['minlength'].requiredLength} caractères`;
    if (field.hasError('hasNumber')) return 'Le nom du plat ne doit pas contenir de chiffres';
    if (field.hasError('notInteger')) return 'Veuillez entrer un nombre entier valide';
    if (field.hasError('minValue'))
      return `Le prix minimum est de ${field.errors?.['minValue'].min} FCFA`;

    return '';
  }
}
