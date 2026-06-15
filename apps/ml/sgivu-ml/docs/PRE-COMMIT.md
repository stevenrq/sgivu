# pre-commit — configuración y uso

Este documento resume la configuración de `pre-commit` usada en el servicio
`sgivu-ml`, explica el stack de calidad activo y cómo usarlo localmente y en CI.

**Archivo de configuración**: [.pre-commit-config.yaml](../../../../.pre-commit-config.yaml)

## Hooks activos

- `black` (formateador) — rev fijada en `.pre-commit-config.yaml`.
- `pylint` (linter) — hook local que ejecuta el pylint del venv del servicio.

## Notas importantes

### Versión de black sincronizada

El hook de `black` usa una `rev:` específica en `.pre-commit-config.yaml`. La
versión de `black` en `requirements-dev.txt` debe coincidir exactamente para
evitar que el venv y pre-commit produzcan formateos distintos. Si actualizas uno,
actualiza el otro.

### sys.path del hook de pylint

El hook corre desde la raíz del repo, no desde el directorio del servicio. Para
que los imports `app.*` se resuelvan, el hook inyecta el directorio raíz del
paquete vía `--init-hook`:

```yaml
args:
  - "--rcfile=apps/ml/sgivu-ml/pyproject.toml"
  - "--init-hook=import sys; sys.path.insert(0, 'apps/ml/sgivu-ml')"
```

### Archivos excluidos del lint

El patrón `files:` en el hook de pylint excluye `alembic/` (código generado)
y `scripts/` (banners ASCII, sin convenciones estrictas):

```
^apps/ml/sgivu-ml/(?!alembic/|scripts/).*\.py$
```

## Uso local rápido

1. Instalar hooks (si no están instalados):

```bash
export PATH="$HOME/.local/bin:$PATH"
pre-commit install
```

2. Ejecutar todos los hooks sobre todos los archivos:

```bash
make precommit
# equivalente a: pre-commit run --all-files
```

3. Ejecutar solo el linter de ML:

```bash
cd apps/ml/sgivu-ml
.venv/bin/pylint app/ tests/ --rcfile=pyproject.toml
```

4. Ejecutar solo el formateador:

```bash
cd apps/ml/sgivu-ml
.venv/bin/black .
```

## Solución de problemas comunes

- **pylint E0401 (import-error)**: asegúrate de que el hook incluye el
  `--init-hook` con `sys.path.insert`. Si lo corres manualmente desde la raíz
  del repo, añade la ruta a mano:
  ```bash
  PYTHONPATH=apps/ml/sgivu-ml .venv/bin/pylint app/ tests/
  ```
- **Diferencia de formateo entre venv y pre-commit**: verifica que la versión
  de `black` en `requirements-dev.txt` coincide con la `rev:` del hook en
  `.pre-commit-config.yaml`.
- **Falso positivo de pylint en SQLAlchemy** (`E1101`, `E1102`): están
  desactivados globalmente en `pyproject.toml` (`no-member`, `not-callable`).

## Buenas prácticas

- Ejecuta `make precommit` antes de abrir PRs grandes.
- Mantén `pyproject.toml` sincronizado con los umbrales de pylint (`max-args`,
  `max-locals`, etc.) para evitar sorpresas en CI.

## Recomendación para CI (GitHub Actions)

El workflow `.github/workflows/ci.yml` ya ejecuta black + pylint + pytest para
`sgivu-ml`. Para reproducirlo localmente:

```bash
cd apps/ml/sgivu-ml
.venv/bin/black --check .
.venv/bin/pylint app/ tests/ --rcfile=pyproject.toml
.venv/bin/pytest -q tests/
```

---
