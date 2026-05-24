import os
import sys

sys.path.insert(0, os.path.abspath(".."))

project = "sgivu-ml"
author = "SGIVU"
extensions = [
    "sphinx.ext.autodoc",
    "sphinx.ext.napoleon",
    "sphinx.ext.viewcode",
]

templates_path = ["_templates"]
exclude_patterns = ["_build", "Thumbs.db", ".DS_Store"]

language = "es"
html_theme = "alabaster"

napoleon_numpy_docstring = True
napoleon_google_docstring = False
