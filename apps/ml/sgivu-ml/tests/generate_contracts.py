#!/usr/bin/env python3

"""Genera un CSV sintético de contratos de compra/venta de vehículos"""

import csv
import random
from datetime import datetime, timedelta, date
from pathlib import Path

NUM_CONTRACTS = 1000
OUTPUT_FILE = Path(__file__).parent / "data" / "synthetic_contracts.csv"

TODAY = date.today()
# Rango de fechas: entre 1 y 24 meses atrás para tener datos recientes y con historial suficiente.
MIN_MONTHS_AGO = 1  # más reciente
MAX_MONTHS_AGO = 24  # más antiguo


# Probabilidad de que un contrato sea venta vs compra (más ventas para simular demanda).
SALE_PROBABILITY = 0.6
# Probabilidad de tomar un segmento "popular" para generar mayor densidad por modelo.
POPULAR_SEGMENT_WEIGHT = 0.75
VEHICLE_TYPE_CAR = "Automóvil"


# -------------------------------------------------------------------
# Catálogos ampliados
# -------------------------------------------------------------------

contract_types = ["Compra", "Venta"]
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

vehicle_brands_models = {
    "Yamaha": ["FZ 2.0", "MT-03", "NMAX", "XTZ 250", "R3"],
    "Honda": ["CB 190R", "Wave 110", "XR 150L", "PCX 150"],
    "Bajaj": ["Pulsar 200NS", "Boxer CT100", "Discover 125", "Dominar 400"],
    "Suzuki": ["Gixxer 155", "GN 125", "AX4", "DR 650"],
    "Kawasaki": ["Z400", "Ninja 300", "Versys 650"],
    "Mazda": ["3 Touring", "CX-5", "2 Sedan"],
    "Hyundai": ["i25", "Tucson", "Elantra"],
    "Chevrolet": ["Onix", "Tracker", "Sail", "Spark GT"],
    "Renault": ["Logan", "Sandero", "Duster", "Kwid"],
    "Ford": ["Fiesta", "Ranger", "Explorer"],
}

vehicle_types_by_brand = {
    "Yamaha": "Motocicleta",
    "Honda": "Motocicleta",
    "Bajaj": "Motocicleta",
    "Suzuki": "Motocicleta",
    "Kawasaki": "Motocicleta",
    "Mazda": VEHICLE_TYPE_CAR,
    "Hyundai": VEHICLE_TYPE_CAR,
    "Chevrolet": VEHICLE_TYPE_CAR,
    "Renault": VEHICLE_TYPE_CAR,
    "Ford": VEHICLE_TYPE_CAR,
}

# Constantes para segmentos populares (evita duplicar literales).
POPULAR_YAMAHA_FZ = "FZ 2.0"
POPULAR_YAMAHA_MT = "MT-03"
POPULAR_KAWASAKI_NINJA = "Ninja 300"
POPULAR_MAZDA_3 = "3 Touring"
TARGET_HEAVY_SALES = {
    "brand": "Yamaha",
    "model": POPULAR_YAMAHA_MT,
    "line": "MT",
    "vehicle_type": "Motocicleta",
}
# Ventas mínimas que queremos asegurar por mes para el segmento objetivo.
MIN_TARGET_SALES_PER_MONTH = 5

# Líneas/submodelos por modelo (se usa una variante al azar).
vehicle_lines = {
    "Yamaha": {
        "FZ 2.0": ["FZN-150", "FZ-S", "FZ16"],
        "MT-03": ["MT", "MT ABS", "MT SPORT"],
        "NMAX": ["NMAX 155", "NMAX Connected"],
        "XTZ 250": ["XTZ Adventure", "XTZ Rally"],
        "R3": ["R3 ABS", "R3 Monster"],
    },
    "Honda": {
        "CB 190R": ["CBR-R", "190R"],
        "Wave 110": ["Wave Alpha"],
        "XR 150L": ["XR Work", "XR Trail"],
        "PCX 150": ["PCX Deluxe", "PCX ABS"],
    },
    "Bajaj": {
        "Pulsar 200NS": ["NS", "NS FI"],
        "Boxer CT100": ["CT100 KS", "CT100 ES"],
        "Discover 125": ["Discover", "Discover ST"],
        "Dominar 400": ["Dominar UG", "Dominar Touring"],
    },
    "Suzuki": {
        "Gixxer 155": ["Gixxer", "Gixxer SF"],
        "GN 125": ["GN125", "GN125H"],
        "AX4": ["AX4 Work"],
        "DR 650": ["DR650 Rally", "DR650 SE"],
    },
    "Kawasaki": {
        "Z400": ["Z400 Naked"],
        "Ninja 300": ["Ninja 300 KRT", "Ninja 300 ABS"],
        "Versys 650": ["Versys Tourer", "Versys LT"],
    },
    "Mazda": {
        "3 Touring": ["Touring LX", "Touring Sport"],
        "CX-5": ["CX-5 Grand Touring", "CX-5 Sport"],
        "2 Sedan": ["2 Prime", "2 Grand Touring"],
    },
    "Hyundai": {
        "i25": ["i25 GL", "i25 Sedan"],
        "Tucson": ["Tucson GLS", "Tucson Turbo"],
        "Elantra": ["Elantra Value", "Elantra Limited"],
    },
    "Chevrolet": {
        "Onix": ["Onix LT", "Onix Premier"],
        "Tracker": ["Tracker LS", "Tracker LTZ"],
        "Sail": ["Sail LS", "Sail LT"],
        "Spark GT": ["Spark GT LT", "Spark GT Activ"],
    },
    "Renault": {
        "Logan": ["Logan Zen", "Logan Intens"],
        "Sandero": ["Sandero Life", "Sandero GT"],
        "Duster": ["Duster Zen", "Duster Intens"],
        "Kwid": ["Kwid Outsider", "Kwid Zen"],
    },
    "Ford": {
        "Fiesta": ["Fiesta SE", "Fiesta Titanium"],
        "Ranger": ["Ranger XLS", "Ranger XLT"],
        "Explorer": ["Explorer XLT", "Explorer Limited"],
    },
}

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

# Ajustes de precios base por marca para generar valores realistas.
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

# Estacionalidad más marcada por mes (demanda y precios pueden variar).
seasonality = {
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
# Funciones auxiliares
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


def random_line_for_model(brand: str, model: str) -> str:
    """Devuelve una línea/submodelo plausible para la marca/modelo dado."""
    brand_lines = vehicle_lines.get(brand, {})
    options = brand_lines.get(model) or [model]
    return random.choice(options)


def random_vehicle():
    popular_combos = [
        ("Yamaha", POPULAR_YAMAHA_MT),
        ("Yamaha", POPULAR_YAMAHA_FZ),
        ("Hyundai", "i25"),
        ("Mazda", POPULAR_MAZDA_3),
        ("Ford", "Fiesta"),
        ("Ford", "Explorer"),
        ("Kawasaki", POPULAR_KAWASAKI_NINJA),
        ("Kawasaki", "Z400"),
    ]
    if random.random() < POPULAR_SEGMENT_WEIGHT:
        brand, model = random.choice(popular_combos)
    else:
        brand = random.choice(list(vehicle_brands_models.keys()))
        model = random.choice(vehicle_brands_models[brand])

    plate = random_plate()
    # Línea/submodelo para enriquecer el CSV (aparece en el real).
    line = random_line_for_model(brand, model)
    return brand, model, line, plate, vehicle_types_by_brand[brand]


def random_prices(brand, dt):
    base = brand_base_price.get(brand, 15_000_000)
    # Ajuste estacional y ruido.
    season_mult = seasonality.get(dt.month, 1.0)
    purchase = base * random.uniform(0.8, 1.2) * season_mult
    sell = purchase * random.uniform(1.08, 1.32)
    return f"{purchase:.2f}", f"{sell:.2f}"


def months_range(min_m, max_m):
    """Devuelve tupla (inicio_antiguo, fin_reciente) en base a meses atrás."""
    older = TODAY - timedelta(days=max_m * 30)
    newer = TODAY - timedelta(days=min_m * 30)
    return older, newer


def subtract_months(date_ref: date, months_back: int) -> date:
    """Resta meses preservando el día en un rango seguro (usa día 1-28)."""
    month_index = date_ref.year * 12 + (date_ref.month - 1)
    target_index = month_index - months_back
    target_year = target_index // 12
    target_month = target_index % 12 + 1
    day = min(date_ref.day, 28)
    return date(target_year, target_month, day)


def random_datetime():
    months_span = list(range(MIN_MONTHS_AGO, MAX_MONTHS_AGO + 1))
    weights = [
        seasonality.get(((TODAY.month - m - 1) % 12) + 1, 1.0) for m in months_span
    ]
    months_back = random.choices(months_span, weights=weights, k=1)[0]
    target_date = subtract_months(TODAY, months_back)
    day = random.randint(1, 28)
    dt = date(target_date.year, target_date.month, day)
    return datetime(
        dt.year, dt.month, dt.day, random.randint(0, 23), random.randint(0, 59)
    )


def fmt(dt):
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
        "",
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


def generate_target_segment_sales(writer):
    """Garantiza ventas altas para la Yamaha MT-03 (línea MT) cada mes."""
    for months_back in range(0, 12):  # últimos 12 meses
        month_date = subtract_months(TODAY, months_back)
        sales_this_month = random.randint(
            MIN_TARGET_SALES_PER_MONTH, MIN_TARGET_SALES_PER_MONTH + 3
        )
        for _ in range(sales_this_month):
            day = random.randint(1, 28)
            creation = datetime(
                month_date.year,
                month_date.month,
                day,
                random.randint(0, 23),
                random.randint(0, 59),
            )
            write_contract(
                writer=writer,
                creation=creation,
                contract_type="Venta",
                brand=TARGET_HEAVY_SALES["brand"],
                model=TARGET_HEAVY_SALES["model"],
                line=TARGET_HEAVY_SALES["line"],
                plate=random_plate(),
                vtype=TARGET_HEAVY_SALES["vehicle_type"],
            )


# -------------------------------------------------------------------
# Generación del CSV
# -------------------------------------------------------------------


def generate_csv(path, n):
    # Fecha de referencia: momento de generar el CSV (simula fecha de exportación).
    global TODAY
    TODAY = date.today()

    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)

        writer.writerow(["Periodo", "Periodo: todos los registros disponibles"])

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

        # Ventas forzadas para MT-03 (línea MT) con demanda alta.
        generate_target_segment_sales(writer)

        # Resto de contratos aleatorios para diversidad del dataset.
        for _ in range(n):
            creation = random_datetime()
            brand, model, line, plate, vtype = random_vehicle()

            # Probabilidad de venta con estacionalidad y ruido para generar dispersion.
            sale_prob = SALE_PROBABILITY * seasonality.get(creation.month, 1.0)
            sale_prob += random.uniform(-0.25, 0.25)
            if (brand, model) in [
                ("Yamaha", POPULAR_YAMAHA_MT),
                ("Yamaha", POPULAR_YAMAHA_FZ),
                ("Ford", "Fiesta"),
                ("Ford", "Explorer"),
                ("Hyundai", "i25"),
                ("Mazda", POPULAR_MAZDA_3),
                ("Kawasaki", POPULAR_KAWASAKI_NINJA),
            ]:
                sale_prob *= 1.2
            sale_prob = min(0.95, max(0.3, sale_prob))
            contract_type = "Venta" if random.random() < sale_prob else "Compra"

            write_contract(
                writer=writer,
                creation=creation,
                contract_type=contract_type,
                brand=brand,
                model=model,
                line=line,
                plate=plate,
                vtype=vtype,
            )

    print(f"Archivo generado: {Path(path).resolve()} (incluye ventas forzadas MT-03)")


if __name__ == "__main__":
    generate_csv(OUTPUT_FILE, NUM_CONTRACTS)
