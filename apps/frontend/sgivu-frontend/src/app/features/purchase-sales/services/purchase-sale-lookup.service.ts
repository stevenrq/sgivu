import {
  computed,
  DestroyRef,
  inject,
  Injectable,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, map } from 'rxjs';
import { PersonService } from '../../clients/services/person.service';
import { CompanyService } from '../../clients/services/company.service';
import { UserService } from '../../users/services/user.service';
import { CarService } from '../../vehicles/services/car.service';
import { MotorcycleService } from '../../vehicles/services/motorcycle.service';
import {
  ClientOption,
  mapCarsToVehicles,
  mapCompaniesToClients,
  mapMotorcyclesToVehicles,
  mapPersonsToClients,
  mapUsersToOptions,
  UserOption,
  VehicleOption,
} from '../models/purchase-sale-reference.model';

const sortByLabel = <T extends { label: string }>(a: T, b: T): number =>
  a.label.localeCompare(b.label);

@Injectable({ providedIn: 'root' })
export class PurchaseSaleLookupService {
  private readonly personService = inject(PersonService);
  private readonly companyService = inject(CompanyService);
  private readonly userService = inject(UserService);
  private readonly carService = inject(CarService);
  private readonly motorcycleService = inject(MotorcycleService);

  readonly clients = signal<ClientOption[]>([]);
  readonly users = signal<UserOption[]>([]);
  readonly vehicles = signal<VehicleOption[]>([]);

  readonly clientMap = computed(
    () => new Map<number, ClientOption>(this.clients().map((c) => [c.id, c])),
  );

  readonly userMap = computed(
    () => new Map<number, UserOption>(this.users().map((u) => [u.id, u])),
  );

  readonly vehicleMap = computed(
    () => new Map<number, VehicleOption>(this.vehicles().map((v) => [v.id, v])),
  );

  loadVehiclesOnly(
    destroyRef: DestroyRef,
    onError?: (err: unknown) => void,
  ): void {
    const vehicles$ = forkJoin([
      this.carService.getAll(),
      this.motorcycleService.getAll(),
    ]).pipe(
      map(([cars, motorcycles]) => [
        ...mapCarsToVehicles(cars),
        ...mapMotorcyclesToVehicles(motorcycles),
      ]),
    );

    vehicles$.pipe(takeUntilDestroyed(destroyRef)).subscribe({
      next: (vehicleOptions) => {
        this.vehicles.set(vehicleOptions.slice().sort(sortByLabel));
      },
      error: (err) => onError?.(err),
    });
  }

  loadAll(
    destroyRef: DestroyRef,
    onError?: (err: unknown) => void,
    onComplete?: () => void,
  ): void {
    const clients$ = forkJoin([
      this.personService.getAll(),
      this.companyService.getAll(),
    ]).pipe(
      map(([persons, companies]) => [
        ...mapPersonsToClients(persons),
        ...mapCompaniesToClients(companies),
      ]),
    );

    const users$ = this.userService.getAll().pipe(map(mapUsersToOptions));

    const vehicles$ = forkJoin([
      this.carService.getAll(),
      this.motorcycleService.getAll(),
    ]).pipe(
      map(([cars, motorcycles]) => [
        ...mapCarsToVehicles(cars),
        ...mapMotorcyclesToVehicles(motorcycles),
      ]),
    );

    forkJoin([clients$, users$, vehicles$])
      .pipe(
        finalize(() => onComplete?.()),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe({
        next: ([clientOptions, userOptions, vehicleOptions]) => {
          this.clients.set(clientOptions.slice().sort(sortByLabel));
          this.users.set(userOptions.slice().sort(sortByLabel));
          this.vehicles.set(vehicleOptions.slice().sort(sortByLabel));
        },
        error: (err) => onError?.(err),
      });
  }
}
