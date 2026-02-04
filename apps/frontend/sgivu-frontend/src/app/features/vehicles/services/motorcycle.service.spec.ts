import { TestBed } from '@angular/core/testing';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { MotorcycleService } from './motorcycle.service';
import { environment } from '../../../../environments/environment';
import { Motorcycle } from '../models/motorcycle.model';
import { VehicleStatus } from '../models/vehicle-status.enum';

describe('MotorcycleService', () => {
  let service: MotorcycleService;
  let httpMock: HttpTestingController;

  const buildMotorcycle = (overrides: Partial<Motorcycle> = {}): Motorcycle =>
    ({
      id: 1,
      plate: 'MTR123',
      brand: 'Honda',
      line: 'CB',
      model: '2024',
      motorcycleType: 'Touring',
      transmission: 'Manual',
      year: 2024,
      capacity: 500,
      mileage: 0,
      salePrice: 8000,
      status: VehicleStatus.AVAILABLE,
      cityRegistered: 'Bogotá',
      enabled: true,
      ...overrides,
    }) as Motorcycle;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });

    service = TestBed.inject(MotorcycleService);
    httpMock = TestBed.inject(HttpTestingController);

    // Reiniciar los estados de los signals
    service.getState().set([] as any);
    service.getPagerState().set({} as any);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('create()', () => {
    it('should POST new motorcycle and append to motorcycles state', () => {
      const newMotorcycle = buildMotorcycle({ id: 1, plate: 'XYZ789' });

      let received: any;
      service.create(newMotorcycle).subscribe((m) => (received = m));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newMotorcycle);

      req.flush(newMotorcycle);

      expect(received).toEqual(newMotorcycle);
      const state = service.getState()();
      expect(state).toContain(newMotorcycle);
    });

    it('should append to existing motorcycles state when non-empty', () => {
      const existing = [buildMotorcycle({ id: 2, plate: 'OLD123' })] as any;
      service.getState().set(existing);

      const newMotorcycle = buildMotorcycle({ id: 3, plate: 'NEW456' });
      service.create(newMotorcycle).subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles`);
      req.flush(newMotorcycle);

      const state = service.getState()();
      expect(state.length).toBe(2);
      expect(state.find((m: any) => m.id === 3)).toBeDefined();
    });

    it('should propagate error on POST failure and not modify state', (done) => {
      const initial = [buildMotorcycle({ id: 5 })] as any;
      service.getState().set(initial);

      service.create(buildMotorcycle({ id: 6, plate: 'BAD999' })).subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          const state = service.getState()();
          expect(state).toEqual(initial);
          done();
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles`);
      req.error(new ProgressEvent('error'));
    });
  });

  describe('getAll()', () => {
    it('should GET all motorcycles and set motorcycles state', () => {
      const mock = [
        buildMotorcycle({ id: 1, plate: 'AAA001' }),
        buildMotorcycle({ id: 2, plate: 'AAA002' }),
      ] as any[];

      let received: any[] | undefined;
      service.getAll().subscribe((v) => (received = v));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles`);
      expect(req.request.method).toBe('GET');
      req.flush(mock);

      expect(received).toEqual(mock);
      expect(service.getState()()).toEqual(mock);
    });

    it('should handle empty list response', () => {
      let received: any[] | undefined;
      service.getAll().subscribe((v) => (received = v));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles`);
      req.flush([]);

      expect(received).toEqual([]);
      expect(service.getState()()).toEqual([]);
    });
  });

  describe('getAllPaginated()', () => {
    it('should GET paginated motorcycles and set pager state', () => {
      const mock = {
        page: 2,
        content: [buildMotorcycle({ id: 3, plate: 'AAA003' })],
      } as any;

      let received: any;
      service.getAllPaginated(2).subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/page/2`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mock);

      expect(received).toEqual(mock);
      expect(service.getPagerState()()).toEqual(mock);
    });

    it('should handle empty page results', () => {
      const mock = { page: 1, content: [] } as any;

      let received: any;
      service.getAllPaginated(1).subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/page/1`,
      );
      req.flush(mock);

      expect(received).toEqual(mock);
      expect(service.getPagerState()()).toEqual(mock);
    });
  });

  describe('getCounts()', () => {
    it('should GET motorcycle counts and transform response', () => {
      const mock = {
        totalMotorcycles: 50,
        availableMotorcycles: 40,
        unavailableMotorcycles: 10,
      };

      let received: any;
      service.getCounts().subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/count`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mock);

      expect(received).toEqual({
        total: 50,
        available: 40,
        unavailable: 10,
      });
    });

    it('should transform zero counts correctly', () => {
      const mock = {
        totalMotorcycles: 0,
        availableMotorcycles: 0,
        unavailableMotorcycles: 0,
      };

      let received: any;
      service.getCounts().subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/count`,
      );
      req.flush(mock);

      expect(received).toEqual({
        total: 0,
        available: 0,
        unavailable: 0,
      });
    });
  });

  describe('update()', () => {
    it('should PUT updated motorcycle and update motorcycles state', () => {
      const initial = [
        buildMotorcycle({ id: 1, plate: 'OLD001' }),
        buildMotorcycle({ id: 2, plate: 'KEEP001' }),
      ];
      service.getState().set(initial as any);

      const updated = buildMotorcycle({ id: 1, plate: 'NEW001' });

      let received: any;
      service.update(1, updated).subscribe((v) => (received = v));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/1`);
      expect(req.request.method).toBe('PUT');
      req.flush(updated);

      expect(received).toEqual(updated);
      const state = service.getState()();
      const found = state.find((m: any) => m.id === 1);
      expect(found).toBeDefined();
      expect((found as any).plate).toBe('NEW001');
      expect(state.length).toBe(2);
    });

    it('should not alter state when updating non-existing motorcycle', () => {
      const initial = [buildMotorcycle({ id: 2, plate: 'KEEP001' })] as any;
      service.getState().set(initial);

      const updated = buildMotorcycle({ id: 99, plate: 'GHOST999' });

      let received: any;
      service.update(99, updated).subscribe((v) => (received = v));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/99`);
      req.flush(updated);

      expect(received).toEqual(updated);
      const state = service.getState()();
      expect(state).toEqual(initial);
    });

    it('should not change state on server error', (done) => {
      const initial = [buildMotorcycle({ id: 1, plate: 'OLD001' })] as any;
      service.getState().set(initial);

      service.update(1, buildMotorcycle({ id: 1, plate: 'NEW001' })).subscribe({
        next: () => fail('should error'),
        error: () => {
          expect(service.getState()()).toEqual(initial);
          done();
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/1`);
      req.error(new ProgressEvent('error'));
    });
  });

  describe('changeStatus()', () => {
    it('should PATCH motorcycle status and update motorcycles state', () => {
      const initial = [
        buildMotorcycle({ id: 1, status: VehicleStatus.AVAILABLE }),
      ];
      service.getState().set(initial as any);

      let received: any;
      service
        .changeStatus(1, VehicleStatus.IN_MAINTENANCE)
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/1/status`,
      );
      expect(req.request.method).toBe('PATCH');
      req.flush({ status: VehicleStatus.IN_MAINTENANCE });

      expect(received).toBe(VehicleStatus.IN_MAINTENANCE);
      const state = service.getState()();
      const found = state.find((m: any) => m.id === 1);
      expect((found as any).status).toBe(VehicleStatus.IN_MAINTENANCE);
    });

    it('should handle status change error gracefully', (done) => {
      const initial = [
        buildMotorcycle({ id: 1, status: VehicleStatus.AVAILABLE }),
      ];
      service.getState().set(initial as any);

      service.changeStatus(1, VehicleStatus.IN_MAINTENANCE).subscribe({
        next: () => fail('should error'),
        error: () => {
          const state = service.getState()();
          expect((state[0] as any).status).toBe(VehicleStatus.AVAILABLE);
          done();
        },
      });

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/1/status`,
      );
      req.error(new ProgressEvent('error'));
    });
  });

  describe('delete()', () => {
    it('should DELETE motorcycle and remove from motorcycles state', () => {
      const initial = [
        buildMotorcycle({ id: 1, plate: 'DEL001' }),
        buildMotorcycle({ id: 2, plate: 'KEEP001' }),
      ];
      service.getState().set(initial as any);

      let done = false;
      service.delete(1).subscribe(() => (done = true));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      expect(done).toBeTrue();
      const state = service.getState()();
      expect(state.find((m: any) => m.id === 1)).toBeUndefined();
      expect(state.length).toBe(1);
    });

    it('should leave state unchanged when deleting non-existing id', () => {
      const initial = [buildMotorcycle({ id: 2, plate: 'KEEP001' })] as any;
      service.getState().set(initial);

      let done = false;
      service.delete(3).subscribe(() => (done = true));

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      expect(done).toBeTrue();
      expect(service.getState()()).toEqual(initial);
    });

    it('should not change state on server error', (done) => {
      const initial = [buildMotorcycle({ id: 1, plate: 'DEL001' })] as any;
      service.getState().set(initial);

      service.delete(1).subscribe({
        next: () => fail('should error'),
        error: () => {
          expect(service.getState()()).toEqual(initial);
          done();
        },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/v1/motorcycles/1`);
      req.error(new ProgressEvent('error'));
    });
  });

  describe('search()', () => {
    it('should GET motorcycles matching filters', () => {
      const mock = [buildMotorcycle({ id: 1, brand: 'Honda' })] as any[];

      let received: any[] | undefined;
      service
        .search({ brand: 'Honda', motorcycleType: 'Touring' })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        return (
          request.url === `${environment.apiUrl}/v1/motorcycles/search` &&
          request.params.get('brand') === 'Honda' &&
          request.params.get('motorcycleType') === 'Touring'
        );
      });

      expect(req.request.method).toBe('GET');
      req.flush(mock);

      expect(received).toEqual(mock);
    });

    it('should exclude null and undefined filter values', () => {
      const mock = [] as any[];

      let received: any[] | undefined;
      service
        .search({
          brand: 'Yamaha',
          motorcycleType: undefined,
          transmission: '',
        })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return (
          request.url === `${environment.apiUrl}/v1/motorcycles/search` &&
          params.get('brand') === 'Yamaha' &&
          params.get('motorcycleType') === null &&
          params.get('transmission') === null
        );
      });

      req.flush(mock);

      expect(received).toEqual(mock);
    });

    it('should handle empty search results', () => {
      let received: any[] | undefined;
      service.search({ brand: 'NotExist' }).subscribe((v) => (received = v));

      const req = httpMock.expectOne(
        `${environment.apiUrl}/v1/motorcycles/search?brand=NotExist`,
      );
      req.flush([]);

      expect(received).toEqual([]);
    });
  });

  describe('searchPaginated()', () => {
    it('should GET paginated motorcycles matching filters', () => {
      const mock = {
        page: 1,
        content: [buildMotorcycle({ id: 1, brand: 'Honda' })],
      } as any;

      let received: any;
      service
        .searchPaginated(1, { brand: 'Honda' })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        return (
          request.url ===
            `${environment.apiUrl}/v1/motorcycles/search/page/1` &&
          request.params.get('brand') === 'Honda'
        );
      });

      expect(req.request.method).toBe('GET');
      req.flush(mock);

      expect(received).toEqual(mock);
    });

    it('should exclude null and undefined filters in paginated search', () => {
      const mock = { page: 0, content: [] } as any;

      let received: any;
      service
        .searchPaginated(0, {
          brand: 'Kawasaki',
          motorcycleType: undefined,
          transmission: '',
        })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return (
          request.url ===
            `${environment.apiUrl}/v1/motorcycles/search/page/0` &&
          params.get('brand') === 'Kawasaki' &&
          params.get('motorcycleType') === null &&
          params.get('transmission') === null
        );
      });

      req.flush(mock);

      expect(received).toEqual(mock);
    });

    it('should include numeric range filters in paginated search', () => {
      const mock = { page: 0, content: [] } as any;

      let received: any;
      service
        .searchPaginated(0, {
          brand: 'Ducati',
          minYear: 2020,
          maxYear: 2024,
          minSalePrice: 5000,
          maxSalePrice: 15000,
        })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return (
          request.url ===
            `${environment.apiUrl}/v1/motorcycles/search/page/0` &&
          params.get('brand') === 'Ducati' &&
          params.get('minYear') === '2020' &&
          params.get('maxYear') === '2024' &&
          params.get('minSalePrice') === '5000' &&
          params.get('maxSalePrice') === '15000'
        );
      });

      req.flush(mock);

      expect(received).toEqual(mock);
    });

    it('should handle empty page results', () => {
      const mock = { page: 5, content: [] } as any;

      let received: any;
      service
        .searchPaginated(5, { status: VehicleStatus.IN_MAINTENANCE })
        .subscribe((v) => (received = v));

      const req = httpMock.expectOne((request) => {
        return (
          request.url ===
            `${environment.apiUrl}/v1/motorcycles/search/page/5` &&
          request.params.get('status') === VehicleStatus.IN_MAINTENANCE
        );
      });

      req.flush(mock);

      expect(received.content.length).toBe(0);
    });
  });

  describe('buildSearchParams()', () => {
    it('should build HttpParams from filters excluding empty values', () => {
      const params = (service as any).buildSearchParams({
        brand: 'Honda',
        plate: 'MTR001',
        year: 2024,
      });

      expect(params.get('brand')).toBe('Honda');
      expect(params.get('plate')).toBe('MTR001');
      expect(params.get('year')).toBe('2024');
    });

    it('should exclude undefined, null, and empty string values', () => {
      const params = (service as any).buildSearchParams({
        brand: 'Yamaha',
        line: undefined,
        motorcycleType: null,
        transmission: '',
        model: 'MT',
      });

      expect(params.get('brand')).toBe('Yamaha');
      expect(params.get('model')).toBe('MT');
      expect(params.get('line')).toBeNull();
      expect(params.get('motorcycleType')).toBeNull();
      expect(params.get('transmission')).toBeNull();
    });

    it('should convert numeric values to strings', () => {
      const params = (service as any).buildSearchParams({
        minYear: 2020,
        maxYear: 2024,
        minMileage: 1000,
        maxMileage: 50000,
        minSalePrice: 5000,
      });

      expect(params.get('minYear')).toBe('2020');
      expect(params.get('maxYear')).toBe('2024');
      expect(params.get('minMileage')).toBe('1000');
      expect(params.get('maxMileage')).toBe('50000');
      expect(params.get('minSalePrice')).toBe('5000');
    });

    it('should handle empty filters object', () => {
      const params = (service as any).buildSearchParams({});

      expect(params.keys().length).toBe(0);
    });

    it('should handle zero numeric values', () => {
      const params = (service as any).buildSearchParams({
        minYear: 0,
        minMileage: 0,
        minSalePrice: 0,
      });

      expect(params.get('minYear')).toBe('0');
      expect(params.get('minMileage')).toBe('0');
      expect(params.get('minSalePrice')).toBe('0');
    });
  });
});
