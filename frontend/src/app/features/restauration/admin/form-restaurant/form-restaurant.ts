import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RestaurantService } from '../../services/impl/restaurant.service';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CustomValidators } from '../../../../core/validators/CustomValidators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-form-restaurant',
  imports: [ReactiveFormsModule, RouterLink, CommonModule,FontAwesomeModule],
  templateUrl: './form-restaurant.html',
  styleUrl: './form-restaurant.css',
})
export class FormRestaurant implements OnInit {
  private readonly restaurantService = inject(RestaurantService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  restaurantForm!: FormGroup;
  isSubmitting = false;
  ngOnInit(): void {
    this.restaurantForm = this.fb.group({
      restaurantName: [
        '',
        [Validators.required, Validators.minLength(3), CustomValidators.noNumber],
      ],
      address: [
        '',
        [Validators.required, CustomValidators.noNumber, CustomValidators.noWhitespace],
      ],
      deliveryFee: ['', [Validators.required, CustomValidators.integer]],
      contact: ['', [Validators.required, Validators.minLength(9), CustomValidators.phoneNumberSN]],
    });
  }
  onSubmit(): void {
    if (this.restaurantForm.invalid) {
      this.restaurantForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const formValue = this.restaurantForm.value;

    this.restaurantService.createRestaurant(formValue).subscribe({
      next: () => {
        this.restaurantForm.reset();
        this.router.navigate(['/admin/liste-restaurant']);
      },
      error: (err) => {
        this.isSubmitting = false;
      },
      complete: () => (this.isSubmitting = false),
    });
  }

  hasError(fieldName: string): boolean {
    const field = this.restaurantForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getError(fieldName: string): string {
    const field = this.restaurantForm.get(fieldName);
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
