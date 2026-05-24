# pre-commit — configuración y uso

Este documento resume la configuración de `pre-commit` usada en el servicio
`sgivu-ml`, explica por qué se hizo un ajuste puntual y cómo usarlo
localmente y en CI.

**Archivo de configuración**: [apps/ml/sgivu-ml/.pre-commit-config.yaml](../.pre-commit-config.yaml)

## Hooks activos

- `black` (formateador) — versión especificada en `.pre-commit-config.yaml`.
- `ruff` (linter + autofix) — ejecutado con `--fix`.
- `pydocstyle` (comprobador de docstrings) — convención `numpy`.

## Nota importante sobre `pydocstyle`

Originalmente el hook de `pydocstyle` se configuró para ejecutar
`pydocstyle --convention=numpy apps/ml/sgivu-ml`, lo que provocaba que el
hook hiciera un escaneo completo de todo el árbol en cada commit (lento o
bloqueante). Para evitar que commits se queden "colgados" se aplicaron dos
ajustes:

- Restricción de archivos mediante la clave `files: ^apps/ml/sgivu-ml/app/.*\.py$`:
  el hook solo se ejecuta sobre el código fuente (carpeta `app/`) y no sobre
  tests ni la documentación.
- Uso de `pass_filenames: true` y `entry: pydocstyle --convention=numpy` para
  que `pre-commit` pase sólo los archivos staged al ejecutable. Esto evita
  escaneos innecesarios y mantiene `pydocstyle` rápido durante commits.

Si quieres que `pydocstyle` analice todo el repo (por ejemplo en CI), puedes
ejecutarlo manualmente con:

```bash
# desde la raíz del servicio
source .venv/bin/activate
/.venv/bin/pydocstyle --convention=numpy .
```

## Uso local rápido

1. Activar el entorno virtual del servicio (si existe):

```bash
cd apps/ml/sgivu-ml
source .venv/bin/activate
```

1. Instalar hooks y dependencias (si no están instaladas):

```bash
pip install pre-commit pydocstyle ruff black
pre-commit install
```

1. Ejecutar hooks sobre todos los archivos (chequeo completo):

```bash
pre-commit run --all-files
```

1. Ejecutar sólo el hook de `pydocstyle` (verbose) sobre los staged files:

```bash
pre-commit run pydocstyle --verbose
```

## Solución de problemas comunes

- Si el commit se queda esperando en `pydocstyle`: asegúrate de que el hook
  esté configurado con `pass_filenames: true` y `files:` limitado a `app/`.
- Para errores de docstring (`D103`, `D406`, `D407`, etc.):
  - `D103`: añade docstring a la función pública.
  - `D406/D407`: use la convención NumPy, p.ej.:

    ```py
    Returns
    -------
    int
        Descripción...
    ```

- Para saltarse temporalmente los hooks (no recomendado):

```bash
git commit --no-verify -m "..."
```

## Buenas prácticas

- Ejecuta `pre-commit run --all-files` antes de abrir PRs grandes.
- Mantén `pyproject.toml` sincronizado con las reglas de linters (`ruff`,
  `pydocstyle`) para evitar sorpresas en CI.

## Recomendación para CI (GitHub Actions)

Ejemplo mínimo para ejecutar `black`, `ruff`, `pydocstyle` y tests en CI:

```yaml
name: CI
on: [push, pull_request]
jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v4
        with:
          python-version: '3.12'
      - run: python -m pip install --upgrade pip
      - run: pip install -r apps/ml/sgivu-ml/requirements.txt
      - run: pip install pre-commit pydocstyle ruff black pytest
      - run: pre-commit run --all-files
      - run: pydocstyle --convention=numpy apps/ml/sgivu-ml
      - run: python -m pytest -q apps/ml/sgivu-ml/tests
```

---
