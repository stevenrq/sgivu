import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NgClass } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { FormShellComponent } from '../../../../shared/components/form-shell/form-shell.component';
import {
  lengthValidator,
  noSpecialCharactersValidator,
  passwordStrengthValidator,
} from '../../../../shared/validators/form.validator';
import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../../auth/services/auth.service';
import {
  showErrorAlert,
  showSuccessAlert,
} from '../../../../shared/utils/swal-alert.utils';
import {
  AddressFormControls,
  buildAddressFormGroup,
  normalizeAddress,
} from '../../../../shared/utils/address-form.utils';
import {
  SubmitConfig,
  SubmitCopy,
  ViewCopy,
  composeSubmitConfig,
} from '../../../../shared/models/form-config.model';

interface UserFormControls {
  nationalId: FormControl<string | null>;
  firstName: FormControl<string | null>;
  lastName: FormControl<string | null>;
  phoneNumber: FormControl<string | null>;
  email: FormControl<string | null>;
  address: FormGroup<AddressFormControls>;
  username: FormControl<string | null>;
  password: FormControl<string | null>;
}

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, NgClass, FormShellComponent],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserFormComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  formGroup: FormGroup<UserFormControls> = this.buildForm();
  showPassword = false;
  isEditMode = false;
  readonly submitting = signal(false);

  private currentUserId: number | null = null;
  private currentAddressId: number | null = null;

  readonly loading = signal(false);

  private readonly submitCopy: SubmitCopy = {
    createSuccess: 'El usuario fue registrado exitosamente.',
    updateSuccess: 'La información del usuario fue actualizada exitosamente.',
    createError:
      'No se pudo registrar el usuario. Intenta nuevamente en unos momentos.',
    updateError:
      'No se pudo actualizar el usuario. Intenta nuevamente en unos momentos.',
    redirectCommand: ['/users/page', 0],
  };

  private readonly viewCopy: ViewCopy = {
    createTitle: 'Crear Nuevo Usuario',
    editTitle: 'Editar Usuario',
    createSubtitle:
      'Completa el formulario para registrar un nuevo usuario en el sistema.',
    editSubtitle: 'Modifica los datos del usuario seleccionado.',
  };

  private readonly initialValue = {
    nationalId: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    email: '',
    address: {
      street: '',
      number: '',
      city: '',
    },
    username: '',
    password: '',
  };

  get headerIcon(): string {
    return this.isEditMode ? 'bi-pencil-square' : 'bi-person-plus-fill';
  }

  get titleText(): string {
    return this.isEditMode
      ? this.viewCopy.editTitle
      : this.viewCopy.createTitle;
  }

  get subtitleText(): string {
    return this.isEditMode
      ? this.viewCopy.editSubtitle
      : this.viewCopy.createSubtitle;
  }

  ngOnInit(): void {
    this.configurePasswordWatcher();
    const isSelfEdit = this.route.snapshot.data?.['selfEdit'] === true;

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const idParam = params.get('id');
        if (idParam) {
          const id = Number(idParam);
          if (Number.isNaN(id)) {
            void showErrorAlert('El identificador proporcionado no es válido.');
            void this.router.navigate(['/users/page', 0]);
            return;
          }
          this.activateEditMode(id);
          return;
        }

        if (isSelfEdit) {
          const currentUserId = this.authService.getUserId();
          if (currentUserId != null) {
            this.activateEditMode(currentUserId);
            return;
          }
          void showErrorAlert('No se pudo identificar al usuario actual.');
          void this.router.navigate(['/login']);
          return;
        }

        this.resetToCreateMode();
      });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  protected onSubmit(): void {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }

    const { request$, successMessage, errorMessage, redirectCommand } =
      this.buildSubmitConfig();

    this.submitting.set(true);

    request$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => {
        void showSuccessAlert(successMessage);
        void this.router.navigate(redirectCommand);
      },
      error: () => {
        void showErrorAlert(errorMessage);
      },
    });
  }

  protected onCancel(): void {
    void this.router.navigate(this.submitCopy.redirectCommand);
  }

  private buildForm(): FormGroup<UserFormControls> {
    return this.formBuilder.group({
      nationalId: new FormControl<string | null>('', [
        Validators.required,
        Validators.pattern(/^\d+$/),
        lengthValidator(7, 10),
      ]),
      firstName: new FormControl<string | null>('', [
        Validators.required,
        lengthValidator(3, 20),
      ]),
      lastName: new FormControl<string | null>('', [
        Validators.required,
        lengthValidator(3, 20),
      ]),
      phoneNumber: new FormControl<string | null>('', [
        Validators.required,
        Validators.pattern(/^\d+$/),
        lengthValidator(10, 10),
      ]),
      email: new FormControl<string | null>('', [
        Validators.required,
        Validators.email,
        lengthValidator(16, 40),
      ]),
      address: buildAddressFormGroup(this.formBuilder, {
        street: { min: 5, max: 50 },
        number: { min: 1, max: 10 },
        city: { min: 4, max: 30 },
      }),
      username: new FormControl<string | null>('', [
        Validators.required,
        lengthValidator(6, 20),
        noSpecialCharactersValidator(),
      ]),
      password: new FormControl<string | null>('', [
        Validators.required,
        lengthValidator(6, 20),
        passwordStrengthValidator(),
      ]),
    });
  }

  private configurePasswordWatcher(): void {
    const passwordControl = this.formGroup.controls.password;

    passwordControl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        if (!this.isEditMode) return;

        if (value) {
          passwordControl.setValidators([
            lengthValidator(6, 20),
            passwordStrengthValidator(),
          ]);
        } else {
          passwordControl.clearValidators();
        }

        passwordControl.updateValueAndValidity({ emitEvent: false });
      });
  }

  private activateEditMode(id: number): void {
    if (this.currentUserId === id && this.isEditMode) return;

    this.isEditMode = true;
    this.currentUserId = id;
    this.applyEditPasswordBehaviour();
    this.loadUserForEdit(id);
  }

  private loadUserForEdit(id: number): void {
    this.loading.set(true);

    this.userService
      .getById(id)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (user) => {
          this.currentAddressId = user.address?.id ?? null;
          this.formGroup.patchValue({
            nationalId: user.nationalId?.toString() ?? '',
            firstName: user.firstName ?? '',
            lastName: user.lastName ?? '',
            phoneNumber: user.phoneNumber?.toString() ?? '',
            email: user.email ?? '',
            address: {
              street: user.address?.street ?? '',
              number: user.address?.number ?? '',
              city: user.address?.city ?? '',
            },
            username: user.username ?? '',
            password: '',
          });
          this.formGroup.markAsPristine();
          this.formGroup.markAsUntouched();
        },
        error: () => {
          void showErrorAlert(
            'No se pudo cargar la información del usuario solicitado.',
          );
          void this.router.navigate(this.submitCopy.redirectCommand);
        },
      });
  }

  private resetToCreateMode(): void {
    this.isEditMode = false;
    this.currentUserId = null;
    this.currentAddressId = null;
    this.formGroup.reset(this.initialValue);
    this.applyCreatePasswordBehaviour();
    this.formGroup.markAsPristine();
    this.formGroup.markAsUntouched();
  }

  private applyEditPasswordBehaviour(): void {
    const pw = this.formGroup.controls.password;
    pw.setValue('', { emitEvent: false });
    pw.clearValidators();
    pw.updateValueAndValidity({ emitEvent: false });
  }

  private applyCreatePasswordBehaviour(): void {
    const pw = this.formGroup.controls.password;
    pw.setValidators([
      Validators.required,
      lengthValidator(6, 20),
      passwordStrengthValidator(),
    ]);
    pw.updateValueAndValidity({ emitEvent: false });
  }

  private buildSubmitConfig(): SubmitConfig {
    const payload = this.buildUserPayload();

    const request$ =
      this.isEditMode && this.currentUserId != null
        ? this.userService.update(this.currentUserId, payload as User)
        : this.userService.create(payload as User);

    return composeSubmitConfig(this.submitCopy, request$, this.isEditMode);
  }

  private buildUserPayload(): Partial<User> {
    const raw = this.formGroup.getRawValue();
    const payload: Partial<User> = {
      nationalId: this.toNumber(raw.nationalId),
      firstName: raw.firstName?.trim() ?? '',
      lastName: raw.lastName?.trim() ?? '',
      phoneNumber: this.toNumber(raw.phoneNumber),
      email: raw.email?.trim() ?? '',
      username: raw.username?.trim().toLowerCase() ?? '',
      password: raw.password?.trim() || undefined,
      address: normalizeAddress(
        this.formGroup.controls.address,
        this.isEditMode ? this.currentAddressId : null,
      ),
    };

    if (!payload.password) {
      delete payload.password;
    }

    if (this.isEditMode && this.currentUserId != null) {
      payload.id = this.currentUserId;
    }

    return payload;
  }

  private toNumber(value: string | null | undefined): number | undefined {
    if (!value) return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
}
