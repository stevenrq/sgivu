-- Demo data — development environment only
-- Inventario sintético alineado con el seed de purchase-sale para entrenar el
-- modelo de demanda mensual del microservicio sgivu-ml. La generación es
-- determinista (sin setseed-dependent randomness en cantidades) para que
-- vehicle_id = purchase.id en ambos seeds.
--
-- Modelo de demanda compartido (igual fórmula en
-- apps/backend/sgivu-purchase-sale/.../R__demo_data.sql y en
-- apps/ml/sgivu-ml/tests/generate_contracts.py):
--   ventas_objetivo(s, mb) = max(1, round(
--       v_base[s]
--       × v_seasonality[mes_calendario(now(), mb)]
--       × (1 + 0.005 × (v_horizon - mb))
--       × (0.85 + 0.30 × pseudo_random(s, mb))
--   ))
--   pseudo_random(s, mb) = ((s × 7919 + mb × 31337) % 1000) / 1000.0
--
-- Convención cross-service: ids 1..N donde N = sum_{s,mb} (n_sales(s,mb) + 1).
-- Los primeros n_sales(s,mb) vehículos de cada celda están "SOLD" (vendidos),
-- el último es inventario disponible. La enumeración es por (mb DESC, s, instance)
-- para que el primer id corresponda al mes más antiguo del segmento 1.
--
-- Idempotente: TRUNCATE ... RESTART IDENTITY CASCADE antes de re-insertar.

DO
$$
    DECLARE
        -- Catálogo de 39 segmentos: 19 motos (idx 1..19) + 20 autos (idx 20..39).
        v_brand        TEXT[] := ARRAY [
            'Yamaha','Yamaha','Yamaha','Yamaha',
            'Honda','Honda','Honda','Honda',
            'Bajaj','Bajaj','Bajaj','Bajaj',
            'Suzuki','Suzuki','Suzuki','Suzuki',
            'Kawasaki','Kawasaki','Kawasaki',
            'Mazda','Mazda','Mazda','Mazda',
            'Hyundai','Hyundai','Hyundai','Hyundai',
            'Chevrolet','Chevrolet','Chevrolet','Chevrolet',
            'Renault','Renault','Renault','Renault',
            'Ford','Ford','Ford','Ford'
            ];
        v_model        TEXT[] := ARRAY [
            'FZ 2.0','FZ 2.0','NMAX','R3',
            'CB 190R','Wave 110','XR 150L','PCX 150',
            'Pulsar 200NS','Boxer CT100','Discover 125','Dominar 400',
            'Gixxer 155','GN 125','AX4','DR 650',
            'Z400','Ninja 300','Versys 650',
            '3 Touring','3 Touring','CX-5','2 Sedan',
            'i25','i25','Tucson','Elantra',
            'Onix','Tracker','Sail','Spark GT',
            'Logan','Sandero','Duster','Kwid',
            'Fiesta','Fiesta','Ranger','Explorer'
            ];
        v_line         TEXT[] := ARRAY [
            'FZN-150','FZ-S','NMAX 155','R3 ABS',
            'CBR-R','Wave Alpha','XR Work','PCX Deluxe',
            'NS','CT100 KS','Discover','Dominar UG',
            'Gixxer','GN125','AX4 Work','DR650 Rally',
            'Z400 Naked','Ninja 300 KRT','Versys LT',
            'Touring LX','Touring Sport','CX-5 Sport','2 Prime',
            'i25 GL','i25 Sedan','Tucson GLS','Elantra Value',
            'Onix LT','Tracker LS','Sail LS','Spark GT LT',
            'Logan Zen','Sandero Life','Duster Zen','Kwid Zen',
            'Fiesta SE','Fiesta Titanium','Ranger XLS','Explorer XLT'
            ];
        -- Demanda mensual base por segmento (ventas/mes esperadas, antes de
        -- estacionalidad/tendencia/ruido). Calibrada para producir ~5.500 SALEs
        -- y ~7.000 PURCHASEs sobre 36 meses (~12.500 contratos en total).
        v_base         INT[]    := ARRAY [
            6,4,4,2, 5,6,3,2, 4,6,4,2, 3,4,2,2, 2,4,2,
            3,4,5,2, 5,4,3,2, 4,3,2,3, 3,4,4,2, 3,2,3,4
            ];
        -- Multiplicador estacional por mes calendario (1..12). Fuente:
        -- generate_contracts.py:283-296. Pico en diciembre, valle en enero.
        v_seasonality  NUMERIC[] := ARRAY [
            0.70, 0.80, 0.95, 1.10, 1.20, 1.25,
            1.10, 0.90, 0.75, 0.85, 1.05, 1.30
            ];
        v_horizon      INT      := 36;
        v_moto_cutoff  INT      := 19;
        v_colors       TEXT[]   := ARRAY ['Blanco','Rojo','Azul','Gris','Negro','Plateado','Verde','Naranja'];
        v_cities       TEXT[]   := ARRAY ['Medellín','Bogotá','Cali','Barranquilla','Bucaramanga','Pereira','Cartagena','Manizales'];
        v_car_bodies   TEXT[]   := ARRAY ['SEDAN','HATCHBACK','SUV','WAGON'];
        v_car_fuels    TEXT[]   := ARRAY ['GASOLINA','DIESEL','HIBRIDO'];
        v_moto_types   TEXT[]   := ARRAY ['NAKED','SPORT','TOURING','SCOOTER','DUAL'];
        v_now          TIMESTAMPTZ := NOW();
    BEGIN
        PERFORM setseed(0.42);

        TRUNCATE vehicles, cars, motorcycles RESTART IDENTITY CASCADE;

        -- ============================================================
        -- Inventario: por cada (segmento s, mes m_back) emitir
        -- n_sales(s, mb) + 1 vehículos. Los primeros n_sales son SOLD;
        -- el último es inventario disponible (AVAILABLE / IN_USE / etc.).
        -- ============================================================
        INSERT INTO vehicles (id, brand, model, capacity, line, plate, motor_number, serial_number, chassis_number,
                              color, city_registered, year, mileage, transmission, status, purchase_price, sale_price)
        WITH
        seg_month AS (
            SELECT
                s_idx,
                mb,
                v_base[s_idx] AS base,
                ((extract(month from v_now)::int - mb - 1) % 12 + 12) % 12 + 1 AS cal_month
            FROM
                generate_series(1, 39) AS s_idx,
                generate_series(1, v_horizon) AS mb
        ),
        cell AS (
            SELECT
                s_idx, mb, base,
                GREATEST(1, ROUND(
                    base
                    * v_seasonality[cal_month]
                    * (1.0 + 0.005 * (v_horizon - mb))
                    * (0.85 + 0.30 * (((s_idx * 7919 + mb * 31337) % 1000)::numeric / 1000.0))
                ))::int AS n_sales
            FROM seg_month
        ),
        expanded AS (
            SELECT
                c.s_idx, c.mb, c.n_sales, instance,
                row_number() OVER (ORDER BY c.mb DESC, c.s_idx, instance) AS id
            FROM cell c, LATERAL generate_series(1, c.n_sales + 1) AS instance
        )
        SELECT
            e.id,
            v_brand[e.s_idx],
            v_model[e.s_idx],
            CASE WHEN e.s_idx <= v_moto_cutoff THEN 2 ELSE 5 END,
            v_line[e.s_idx],
            'DEMO-' || lpad(e.id::TEXT, 5, '0'),
            'MTR-'  || lpad(e.id::TEXT, 9, '0'),
            'SER-'  || lpad(e.id::TEXT, 9, '0'),
            'CHS-'  || lpad(e.id::TEXT, 9, '0'),
            v_colors[1 + (e.id % 8)],
            v_cities[1 + ((e.id * 3) % 8)],
            2018 + (e.id % 8),
            ((e.id * 257) % 120000),
            CASE
                WHEN e.s_idx <= v_moto_cutoff THEN 'MANUAL'
                WHEN (e.id % 2) = 0 THEN 'AUTOMATICO'
                ELSE 'MANUAL'
            END,
            -- instance ∈ [1, n_sales] → SOLD; instance = n_sales+1 → inventario.
            CASE
                WHEN e.instance <= e.n_sales THEN 'SOLD'
                ELSE (ARRAY['AVAILABLE','AVAILABLE','AVAILABLE','IN_USE','IN_MAINTENANCE'])[1 + (e.id % 5)]
            END,
            ((CASE v_brand[e.s_idx]
                WHEN 'Yamaha'    THEN 12000000
                WHEN 'Honda'     THEN 12000000
                WHEN 'Bajaj'     THEN  9000000
                WHEN 'Suzuki'    THEN 11000000
                WHEN 'Kawasaki'  THEN 20000000
                WHEN 'Mazda'     THEN 45000000
                WHEN 'Hyundai'   THEN 40000000
                WHEN 'Chevrolet' THEN 35000000
                WHEN 'Renault'   THEN 32000000
                WHEN 'Ford'      THEN 50000000
            END) * (0.85 + random() * 0.30))::NUMERIC(12, 2),
            ((CASE v_brand[e.s_idx]
                WHEN 'Yamaha'    THEN 12000000
                WHEN 'Honda'     THEN 12000000
                WHEN 'Bajaj'     THEN  9000000
                WHEN 'Suzuki'    THEN 11000000
                WHEN 'Kawasaki'  THEN 20000000
                WHEN 'Mazda'     THEN 45000000
                WHEN 'Hyundai'   THEN 40000000
                WHEN 'Chevrolet' THEN 35000000
                WHEN 'Renault'   THEN 32000000
                WHEN 'Ford'      THEN 50000000
            END) * (1.10 + random() * 0.25))::NUMERIC(12, 2)
        FROM expanded e;

        -- ============================================================
        -- Subtabla cars: marcas Mazda/Hyundai/Chevrolet/Renault/Ford
        -- ============================================================
        INSERT INTO cars (vehicle_id, body_type, fuel_type, number_of_doors)
        SELECT id,
               v_car_bodies[1 + (id % 4)],
               v_car_fuels[1 + (id % 3)],
               CASE WHEN v_car_bodies[1 + (id % 4)] = 'HATCHBACK' THEN 5 ELSE 4 END
        FROM vehicles
        WHERE brand IN ('Mazda', 'Hyundai', 'Chevrolet', 'Renault', 'Ford');

        -- ============================================================
        -- Subtabla motorcycles: marcas Yamaha/Honda/Bajaj/Suzuki/Kawasaki
        -- ============================================================
        INSERT INTO motorcycles (vehicle_id, motorcycle_type)
        SELECT id,
               v_moto_types[1 + (id % 5)]
        FROM vehicles
        WHERE brand IN ('Yamaha', 'Honda', 'Bajaj', 'Suzuki', 'Kawasaki');

        -- Sincronizar secuencia con el último id generado.
        PERFORM setval('vehicles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM vehicles));
    END
$$;
