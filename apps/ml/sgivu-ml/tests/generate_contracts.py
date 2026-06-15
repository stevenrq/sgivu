#!/usr/bin/env python3

"""Genera un CSV sintético de contratos de compra/venta de vehículos.

La fórmula de demanda y los volúmenes deben mantenerse alineados con los seeds
SQL de Flyway (apps/backend/sgivu-purchase-sale/.../R__demo_data.sql y
apps/backend/sgivu-vehicle/.../R__demo_data.sql) para que la demo offline y el
pipeline E2E vean exactamente el mismo dataset estructural.

Modelo de demanda compartido:
    ventas_objetivo(s, mb) = max(1, round(
        BASE_DEMAND[s]
        * SEASONALITY[mes_calendario(now() - mb meses)]
        * (1 + 0.005 * (HORIZON_MONTHS - mb))
        * (0.85 + 0.30 * pseudo_noise(s, mb))
    ))
    pseudo_noise(s, mb) = ((s * 7919 + mb * 31337) % 1000) / 1000.0
    n_purchases(s, mb)  = ventas_objetivo(s, mb) + 1   (1 vehículo de inventario)

Las constantes se mantienen sincronizadas manualmente con el SQL; cambiar una
sin la otra rompe la equivalencia entre ambiente local (CSV) y BD (Flyway).
"""

import csv
import random
from datetime import date, datetime, timedelta
from pathlib import Path

OUTPUT_FILE = Path(__file__).parent / "data" / "synthetic_contracts.csv"

TODAY = date.today()

# Horizonte de generación: 36 meses hacia atrás desde hoy.
HORIZON_MONTHS = 36

# Catálogo de 39 segmentos (idéntico al de los seeds SQL).
# Cada segmento es (brand, model, line, vehicle_type).
VEHICLE_TYPE_CAR = "Automóvil"
VEHICLE_TYPE_MOTO = "Motocicleta"

SEGMENTS: list[tuple[str, str, str, str]] = [
    # Yamaha (4)
    ("Yamaha", "FZ 2.0", "FZN-150", VEHICLE_TYPE_MOTO),
    ("Yamaha", "FZ 2.0", "FZ-S", VEHICLE_TYPE_MOTO),
    ("Yamaha", "NMAX", "NMAX 155", VEHICLE_TYPE_MOTO),
    ("Yamaha", "R3", "R3 ABS", VEHICLE_TYPE_MOTO),
    # Honda (4)
    ("Honda", "CB 190R", "CBR-R", VEHICLE_TYPE_MOTO),
    ("Honda", "Wave 110", "Wave Alpha", VEHICLE_TYPE_MOTO),
    ("Honda", "XR 150L", "XR Work", VEHICLE_TYPE_MOTO),
    ("Honda", "PCX 150", "PCX Deluxe", VEHICLE_TYPE_MOTO),
    # Bajaj (4)
    ("Bajaj", "Pulsar 200NS", "NS", VEHICLE_TYPE_MOTO),
    ("Bajaj", "Boxer CT100", "CT100 KS", VEHICLE_TYPE_MOTO),
    ("Bajaj", "Discover 125", "Discover", VEHICLE_TYPE_MOTO),
    ("Bajaj", "Dominar 400", "Dominar UG", VEHICLE_TYPE_MOTO),
    # Suzuki (4)
    ("Suzuki", "Gixxer 155", "Gixxer", VEHICLE_TYPE_MOTO),
    ("Suzuki", "GN 125", "GN125", VEHICLE_TYPE_MOTO),
    ("Suzuki", "AX4", "AX4 Work", VEHICLE_TYPE_MOTO),
    ("Suzuki", "DR 650", "DR650 Rally", VEHICLE_TYPE_MOTO),
    # Kawasaki (3)
    ("Kawasaki", "Z400", "Z400 Naked", VEHICLE_TYPE_MOTO),
    ("Kawasaki", "Ninja 300", "Ninja 300 KRT", VEHICLE_TYPE_MOTO),
    ("Kawasaki", "Versys 650", "Versys LT", VEHICLE_TYPE_MOTO),
    # Mazda (4)
    ("Mazda", "3 Touring", "Touring LX", VEHICLE_TYPE_CAR),
    ("Mazda", "3 Touring", "Touring Sport", VEHICLE_TYPE_CAR),
    ("Mazda", "CX-5", "CX-5 Sport", VEHICLE_TYPE_CAR),
    ("Mazda", "2 Sedan", "2 Prime", VEHICLE_TYPE_CAR),
    # Hyundai (4)
    ("Hyundai", "i25", "i25 GL", VEHICLE_TYPE_CAR),
    ("Hyundai", "i25", "i25 Sedan", VEHICLE_TYPE_CAR),
    ("Hyundai", "Tucson", "Tucson GLS", VEHICLE_TYPE_CAR),
    ("Hyundai", "Elantra", "Elantra Value", VEHICLE_TYPE_CAR),
    # Chevrolet (4)
    ("Chevrolet", "Onix", "Onix LT", VEHICLE_TYPE_CAR),
    ("Chevrolet", "Tracker", "Tracker LS", VEHICLE_TYPE_CAR),
    ("Chevrolet", "Sail", "Sail LS", VEHICLE_TYPE_CAR),
    ("Chevrolet", "Spark GT", "Spark GT LT", VEHICLE_TYPE_CAR),
    # Renault (4)
    ("Renault", "Logan", "Logan Zen", VEHICLE_TYPE_CAR),
    ("Renault", "Sandero", "Sandero Life", VEHICLE_TYPE_CAR),
    ("Renault", "Duster", "Duster Zen", VEHICLE_TYPE_CAR),
    ("Renault", "Kwid", "Kwid Zen", VEHICLE_TYPE_CAR),
    # Ford (4)
    ("Ford", "Fiesta", "Fiesta SE", VEHICLE_TYPE_CAR),
    ("Ford", "Fiesta", "Fiesta Titanium", VEHICLE_TYPE_CAR),
    ("Ford", "Ranger", "Ranger XLS", VEHICLE_TYPE_CAR),
    ("Ford", "Explorer", "Explorer XLT", VEHICLE_TYPE_CAR),
]

# Demanda mensual base por segmento (ventas/mes esperadas).
BASE_DEMAND: list[int] = [
    6,
    4,
    4,
    2,  # Yamaha
    5,
    6,
    3,
    2,  # Honda
    4,
    6,
    4,
    2,  # Bajaj
    3,
    4,
    2,
    2,  # Suzuki
    2,
    4,
    2,  # Kawasaki
    3,
    4,
    5,
    2,  # Mazda
    5,
    4,
    3,
    2,  # Hyundai
    4,
    3,
    2,
    3,  # Chevrolet
    3,
    4,
    4,
    2,  # Renault
    3,
    2,
    3,
    4,  # Ford
]

assert len(SEGMENTS) == 39 and len(BASE_DEMAND) == 39

# Estacionalidad por mes calendario (igual que el SQL: pico en diciembre,
# valle en enero).
SEASONALITY: dict[int, float] = {
    1: 0.70,
    2: 0.80,
    3: 0.95,
    4: 1.10,
    5: 1.20,
    6: 1.25,
    7: 1.10,
    8: 0.90,
    9: 0.75,
    10: 0.85,
    11: 1.05,
    12: 1.30,
}

# -------------------------------------------------------------------
# Pools y catálogos auxiliares (clientes, usuarios, métodos de pago)
# -------------------------------------------------------------------

contract_statuses = ["Activa"]

client_types = ["Empresa", "Persona natural"]

company_clients = [
    "AutosPlus SAS",
    "AutoNorte SAS",
    "AutoFénix SAS",
    "Autos del Valle SAS",
    "AutoLine Ltda",
    "Vehículos Córdoba SAS",
    "Motors del Caribe SAS",
    "RápidoAuto SAS",
    "MotoExpress SAS",
    "Andina Motors SAS",
    "OrienteCar SAS",
]

person_clients_first_names = [
    "Carlos",
    "María",
    "Javier",
    "Ana",
    "Luis",
    "Daniel",
    "Laura",
    "Andrés",
    "Camila",
    "Felipe",
    "Sofía",
    "Valentina",
    "Mateo",
]

person_clients_last_names = [
    "García",
    "Ramírez",
    "González",
    "Pérez",
    "Flores",
    "Rodríguez",
    "Fernández",
    "Ruíz",
    "Martínez",
    "Castro",
    "Salazar",
]

users_responsables = [
    ("Javier Perez", "javierperez", "javier.perez@empresa.com"),
    ("David Ramirez", "davidramirez", "david.ramirez@empresa.com"),
    ("Luis Gonzalez", "luisgonzalez", "luis.gonzalez@empresa.com"),
    ("Daniel Flores", "danielflores", "daniel.flores@empresa.com"),
    ("Steven Quiñones", "steven", "steven@empresa.com"),
    ("Andrea Camacho", "andrea", "andrea.camacho@empresa.com"),
    ("Pedro Cárdenas", "pedroc", "pedro.cardenas@empresa.com"),
    ("Laura Silva", "lauras", "laura.silva@empresa.com"),
    ("Felipe Andrade", "felipea", "felipe.andrade@empresa.com"),
    ("Sara Ríos", "sararios", "sara.rios@empresa.com"),
    ("Manuel Rojas", "mrojas", "manuel.rojas@empresa.com"),
    ("Carmen Ruiz", "cruiz", "carmen.ruiz@empresa.com"),
    ("Verónica Duarte", "vduarte", "veronica.duarte@empresa.com"),
    ("Oscar Castillo", "ocastillo", "oscar.castillo@empresa.com"),
    ("Paula Méndez", "pmendez", "paula.mendez@empresa.com"),
]

payment_methods = [
    "Efectivo",
    "Transferencia bancaria",
    "Crédito",
    "Tarjeta débito",
    "Tarjeta crédito",
    "Mixto",
]

payment_terms = [
    "Pago a una cuota",
    "Pago contra entrega y verificación de documentos",
    "100% contra entrega documental",
    "Inmediato",
    "50% al firmar, 50% contra entrega de documentos",
    "Pago en dos cuotas",
]

payment_limitations = [
    "Ninguna",
    "Pago inmediato, no se aceptan transferencias",
    "Sin cuotas; pago único",
    "No se aceptan cheques de terceros",
    "Pago sujeto a validación bancaria",
]

vehicle_statuses = {
    "Venta": {
        "labels": [
            "Vendido",
            "Disponible",
            "En uso",
            "En mantenimiento",
            "En reparación",
            "Inactivo",
        ],
        "weights": [0.52, 0.20, 0.10, 0.08, 0.07, 0.03],
    },
    "Compra": {
        "labels": [
            "Disponible",
            "En mantenimiento",
            "En reparación",
            "En uso",
            "Inactivo",
            "Vendido",
        ],
        "weights": [0.44, 0.20, 0.14, 0.12, 0.07, 0.03],
    },
}

observations_pool = [
    "",
    "Vehículo con SOAT vigente, revisión técnico-mecánica al día.",
    "Pintas menores en bumper delantero.",
    "Sin deudas reportadas en RUNT y SIMIT.",
    "Incluye maletero trasero y defensas laterales.",
    "Rayón en parachoques trasero.",
    "Mantenimiento reciente en concesionario.",
    "Cambio de aceite recién realizado.",
    "Llantas nuevas instaladas hace menos de 2 meses.",
    "Un solo dueño, historial de servicios completo.",
    "Interior en buen estado, sin olores ni tapicería dañada.",
    "Neumáticos con menos de 5.000 km de uso.",
    "Incluye kit de carretera y duplicado de llave.",
]

# Ajustes de precios base por marca (idéntico al SQL).
brand_base_price = {
    "Yamaha": 12_000_000,
    "Honda": 12_000_000,
    "Bajaj": 9_000_000,
    "Suzuki": 11_000_000,
    "Kawasaki": 20_000_000,
    "Mazda": 45_000_000,
    "Hyundai": 40_000_000,
    "Chevrolet": 35_000_000,
    "Renault": 32_000_000,
    "Ford": 50_000_000,
}

# -------------------------------------------------------------------
# Funciones de demanda (mismo modelo que el SQL)
# -------------------------------------------------------------------


def pseudo_noise(s_idx: int, mb: int) -> float:
    """Ruido determinista en [0, 1) por (segmento, mes_back).

    Usa la misma fórmula que los seeds SQL para que ambos generen volúmenes
    coincidentes por celda (segmento × mes).
    """
    return ((s_idx * 7919 + mb * 31337) % 1000) / 1000.0


def subtract_months(date_ref: date, months_back: int) -> date:
    """Resta meses preservando el día (capeado a 28 para evitar fechas inválidas)."""
    month_index = date_ref.year * 12 + (date_ref.month - 1)
    target_index = month_index - months_back
    target_year = target_index // 12
    target_month = target_index % 12 + 1
    day = min(date_ref.day, 28)
    return date(target_year, target_month, day)


def expected_sales(s_idx: int, mb: int) -> int:
    """Ventas objetivo para (segmento s_idx, mes_back mb).

    Idéntico a la fórmula SQL `GREATEST(1, ROUND(...))` en el seed de Flyway.
    """
    base = BASE_DEMAND[s_idx - 1]
    target = subtract_months(TODAY, mb)
    cal_month = target.month
    seasonality_factor = SEASONALITY[cal_month]
    trend_factor = 1.0 + 0.005 * (HORIZON_MONTHS - mb)
    noise = 0.85 + 0.30 * pseudo_noise(s_idx, mb)
    return max(1, round(base * seasonality_factor * trend_factor * noise))


# -------------------------------------------------------------------
# Helpers de identidad sintética
# -------------------------------------------------------------------


def random_nit():
    return f"NIT {random.randint(900000000, 999999999)}"


def random_cc():
    return f"CC {random.randint(10000000, 1999999999)}"


def random_phone():
    return "3" + "".join(str(random.randint(0, 9)) for _ in range(9))


def random_email_from_name(name):
    return name.lower().replace(" ", ".") + "@cliente.com"


def random_company_client():
    name = random.choice(company_clients)
    nit = random_nit()
    email = name.lower().replace(" ", "") + "@empresa.com"
    phone = random_phone()
    return name, nit, email, phone


def random_person_client():
    first = random.choice(person_clients_first_names)
    last = random.choice(person_clients_last_names)
    full_name = f"{first} {last}"
    doc = random_cc()
    email = random_email_from_name(full_name)
    phone = random_phone()
    return full_name, doc, email, phone


def random_plate():
    letters = "".join(random.choice("ABCDEFGHIJKLMNOPQRSTUVWXYZ") for _ in range(3))
    numbers = "".join(str(random.randint(0, 9)) for _ in range(3))
    return f"{letters}-{numbers}"


def random_prices(brand: str, dt: datetime) -> tuple[str, str]:
    base = brand_base_price.get(brand, 15_000_000)
    season_mult = SEASONALITY.get(dt.month, 1.0)
    purchase = base * random.uniform(0.85, 1.15) * season_mult
    sell = purchase * random.uniform(1.10, 1.35)
    return f"{purchase:.2f}", f"{sell:.2f}"


def random_vehicle_status(contract_type: str) -> str:
    config = vehicle_statuses.get(contract_type) or vehicle_statuses["Venta"]
    return random.choices(config["labels"], weights=config["weights"], k=1)[0]


def cell_creation_datetime(mb: int, s_idx: int, instance: int) -> datetime:
    """Construye la fecha de creación dentro del mes m_back para esta celda.

    El offset en días es determinista por (s_idx, instance) — replica la lógica
    del seed SQL `(s_idx*3 + instance*7) % 28`. Las horas vienen de un random
    seedeado para preservar reproducibilidad del CSV.
    """
    target_date = subtract_months(TODAY, mb)
    day = 1 + ((s_idx * 3 + instance * 7) % 28)
    hour = random.randint(0, 23)
    minute = random.randint(0, 59)
    return datetime(target_date.year, target_date.month, day, hour, minute)


def fmt(dt: datetime) -> str:
    return dt.strftime("%d/%m/%Y %H:%M")


def write_contract(
    writer,
    creation: datetime,
    contract_type: str,
    brand: str,
    model: str,
    line: str,
    plate: str,
    vtype: str,
):
    """Escribe un contrato con datos sintéticos generados al vuelo."""
    client_type = random.choice(client_types)
    if client_type == "Empresa":
        client, doc, email, phone = random_company_client()
    else:
        client, doc, email, phone = random_person_client()

    user_name, username, user_email = random.choice(users_responsables)
    pcompra, pventa = random_prices(brand, creation)
    update = creation + timedelta(hours=random.randint(0, 200))

    row = [
        contract_type,
        "Activa",
        client,
        client_type,
        doc,
        email,
        phone,
        user_name,
        username,
        user_email,
        brand,
        model,
        line,
        plate,
        vtype,
        random_vehicle_status(contract_type),
        pcompra,
        pventa,
        random.choice(payment_methods),
        random.choice(payment_terms),
        random.choice(payment_limitations),
        random.choice(observations_pool),
        fmt(creation),
        fmt(update),
    ]
    writer.writerow(row)


# -------------------------------------------------------------------
# Generación del CSV
# -------------------------------------------------------------------


def generate_csv(path):
    """Genera el CSV recorriendo las 39×36 = 1.404 celdas (segmento, mes_back).

    Para cada celda emite `expected_sales` ventas y `expected_sales + 1`
    compras (el +1 es buffer de inventario). Los volúmenes resultantes se
    aproximan a ~5.300 SALE + ~6.700 PURCHASE = ~12.000 contratos sobre 36
    meses, alineados con los seeds SQL.
    """
    global TODAY
    TODAY = date.today()

    random.seed(42)

    Path(path).parent.mkdir(parents=True, exist_ok=True)
    purchase_count = 0
    sale_count = 0
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)

        headers = [
            "Tipo de contrato",
            "Estado del contrato",
            "Cliente",
            "Tipo de cliente",
            "Documento del cliente",
            "Email del cliente",
            "Teléfono del cliente",
            "Usuario responsable",
            "Usuario (username)",
            "Email del usuario",
            "Marca del vehículo",
            "Modelo del vehículo",
            "Línea del vehículo",
            "Placa del vehículo",
            "Tipo de vehículo",
            "Estado del vehículo",
            "Precio de compra",
            "Precio de venta",
            "Método de pago",
            "Términos de pago",
            "Limitaciones de pago",
            "Observaciones",
            "Fecha de creación",
            "Última actualización",
        ]
        writer.writerow(headers)

        for s_idx in range(1, 40):
            brand, model, line, vtype = SEGMENTS[s_idx - 1]
            for mb in range(1, HORIZON_MONTHS + 1):
                n_sales = expected_sales(s_idx, mb)
                n_purchases = n_sales + 1

                # Compras de la celda.
                for instance in range(1, n_purchases + 1):
                    creation = cell_creation_datetime(mb, s_idx, instance)
                    plate = random_plate()
                    write_contract(
                        writer=writer,
                        creation=creation,
                        contract_type="Compra",
                        brand=brand,
                        model=model,
                        line=line,
                        plate=plate,
                        vtype=vtype,
                    )
                    purchase_count += 1

                # Ventas de la celda (fecha = compra + 7..36 días, dentro o
                # fuera del mes).
                for instance in range(1, n_sales + 1):
                    purchase_creation = cell_creation_datetime(mb, s_idx, instance)
                    sale_creation = purchase_creation + timedelta(
                        days=7 + (instance * 7) % 30
                    )
                    plate = random_plate()
                    write_contract(
                        writer=writer,
                        creation=sale_creation,
                        contract_type="Venta",
                        brand=brand,
                        model=model,
                        line=line,
                        plate=plate,
                        vtype=vtype,
                    )
                    sale_count += 1

    print(
        f"Archivo generado: {Path(path).resolve()} "
        f"(PURCHASE={purchase_count}, SALE={sale_count}, "
        f"TOTAL={purchase_count + sale_count})"
    )


if __name__ == "__main__":
    generate_csv(OUTPUT_FILE)
