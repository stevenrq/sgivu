import { TestBed } from '@angular/core/testing';

import {
  DashboardStateService,
  SavedPredictionState,
} from '../services/dashboard-state.service';
import { VehicleKind } from '../../purchase-sales/models/vehicle-kind.enum';

describe('DashboardStateService', () => {
  let service: DashboardStateService;

  const buildState = (): SavedPredictionState => ({
    payload: {
      vehicleType: VehicleKind.CAR,
      brand: 'Ford',
      model: 'Fiesta',
      line: null,
      horizonMonths: 6,
      confidence: 0.9,
    },
    response: {
      predictions: [{ month: '2025-01', demand: 10, lowerCi: 8, upperCi: 12 }],
      modelVersion: 'v1',
    },
    activeSegmentLabel: 'segmento-1',
    quickVehicleTerm: 'Ford Fiesta',
    latestModel: { version: 'v1' },
  });

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DashboardStateService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('setLastPrediction()', () => {
    it('should set last prediction and persist to storage', () => {
      const state = buildState();
      const persistSpy = spyOn(
        service as any,
        'persistToStorage',
      ).and.callThrough();

      service.setLastPrediction(state);

      expect(persistSpy).toHaveBeenCalledWith(state);
      expect(service.getLastPrediction()).toEqual(state);
    });
  });

  describe('getLastPrediction()', () => {
    it('should return in-memory value without reading storage', () => {
      const state = buildState();
      spyOn(service as any, 'readFromStorage').and.callThrough();

      service.setLastPrediction(state);
      const result = service.getLastPrediction();

      expect(result).toEqual(state);
      expect((service as any).readFromStorage).not.toHaveBeenCalled();
    });

    it('should read from storage when in-memory is empty', () => {
      const state = buildState();
      const key = (service as any).storageKey as string;
      localStorage.setItem(key, JSON.stringify(state));

      const result = service.getLastPrediction();

      expect(result).toEqual(state);
    });

    it('should cache result after reading from storage', () => {
      const state = buildState();
      const key = (service as any).storageKey as string;
      localStorage.setItem(key, JSON.stringify(state));

      const first = service.getLastPrediction();
      localStorage.removeItem(key);
      const second = service.getLastPrediction();

      expect(first).toEqual(state);
      expect(second).toEqual(state);
    });
  });

  describe('clear()', () => {
    it('should clear in-memory value and remove from storage', () => {
      const state = buildState();
      const removeSpy = spyOn(
        service as any,
        'removeFromStorage',
      ).and.callThrough();

      service.setLastPrediction(state);
      service.clear();

      expect(removeSpy).toHaveBeenCalled();
      expect(service.getLastPrediction()).toBeNull();
    });
  });

  describe('persistToStorage()', () => {
    it('should save JSON to localStorage when available', () => {
      const state = buildState();
      const key = (service as any).storageKey as string;
      const setSpy = spyOn(localStorage, 'setItem').and.callThrough();

      (service as any).persistToStorage(state);

      expect(setSpy).toHaveBeenCalledWith(key, JSON.stringify(state));
    });

    it('should not throw if localStorage.setItem fails', () => {
      const state = buildState();
      spyOn(localStorage, 'setItem').and.throwError('fail');

      expect(() => (service as any).persistToStorage(state)).not.toThrow();
    });

    it('should do nothing when localStorage is not available', () => {
      const original = Object.getOwnPropertyDescriptor(window, 'localStorage');
      Object.defineProperty(window, 'localStorage', {
        value: undefined,
        configurable: true,
      });

      try {
        expect(() =>
          (service as any).persistToStorage(buildState()),
        ).not.toThrow();
      } finally {
        if (original) {
          Object.defineProperty(window, 'localStorage', original);
        }
      }
    });
  });

  describe('readFromStorage()', () => {
    it('should return null when localStorage is not available', () => {
      const original = Object.getOwnPropertyDescriptor(window, 'localStorage');
      Object.defineProperty(window, 'localStorage', {
        value: undefined,
        configurable: true,
      });

      try {
        expect((service as any).readFromStorage()).toBeNull();
      } finally {
        if (original) {
          Object.defineProperty(window, 'localStorage', original);
        }
      }
    });

    it('should return null when key is missing', () => {
      const result = (service as any).readFromStorage();
      expect(result).toBeNull();
    });

    it('should return parsed value when JSON is valid', () => {
      const state = buildState();
      const key = (service as any).storageKey as string;
      localStorage.setItem(key, JSON.stringify(state));

      const result = (service as any).readFromStorage();

      expect(result).toEqual(state);
    });

    it('should return null and remove storage when JSON is invalid', () => {
      const key = (service as any).storageKey as string;
      localStorage.setItem(key, '{bad-json');

      const removeSpy = spyOn(
        service as any,
        'removeFromStorage',
      ).and.callThrough();

      const result = (service as any).readFromStorage();

      expect(result).toBeNull();
      expect(removeSpy).toHaveBeenCalled();
    });
  });

  describe('removeFromStorage()', () => {
    it('should remove key from localStorage when available', () => {
      const key = (service as any).storageKey as string;
      localStorage.setItem(key, 'value');
      const removeSpy = spyOn(localStorage, 'removeItem').and.callThrough();

      (service as any).removeFromStorage();

      expect(removeSpy).toHaveBeenCalledWith(key);
    });

    it('should not throw if localStorage.removeItem fails', () => {
      spyOn(localStorage, 'removeItem').and.throwError('fail');

      expect(() => (service as any).removeFromStorage()).not.toThrow();
    });

    it('should do nothing when localStorage is not available', () => {
      const original = Object.getOwnPropertyDescriptor(window, 'localStorage');
      Object.defineProperty(window, 'localStorage', {
        value: undefined,
        configurable: true,
      });

      try {
        expect(() => (service as any).removeFromStorage()).not.toThrow();
      } finally {
        if (original) {
          Object.defineProperty(window, 'localStorage', original);
        }
      }
    });
  });
});
