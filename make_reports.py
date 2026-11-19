# -*- coding: utf-8 -*-
"""
make_reports.py
Constructs:
1. PROJECT_REPORT.html
2. PROJECT_REPORT.pdf (via Chrome headless)
3. PROJECT_REPORT.docx (via python-docx)
4. PROJECT_REPORT.md
"""

import os
import sys
import subprocess
import re
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

BASE_DIR = r"E:\project emp\pbl java"
IMG_DIR = os.path.join(BASE_DIR, "images")

# Image paths
IMG_USE_CASE = os.path.join(IMG_DIR, "fig1_use_case_overview.png")
IMG_ACTIVITY = os.path.join(IMG_DIR, "fig2_storage_workflow_activity.png")
IMG_SEQUENCE = os.path.join(IMG_DIR, "fig3_sequence_upload_pipeline.png")
IMG_CLIENT_ARCH = os.path.join(IMG_DIR, "fig4_client_service_arch.png")
IMG_TOPOLOGY = os.path.join(IMG_DIR, "fig5_component_storage_topology.jpg")
IMG_ER = os.path.join(IMG_DIR, "fig6_database_er_diagram.png")
IMG_CICD = os.path.join(IMG_DIR, "fig7_cicd_release_pipeline.png")

print("Checking image existence:")
for p in [IMG_USE_CASE, IMG_ACTIVITY, IMG_SEQUENCE, IMG_CLIENT_ARCH, IMG_TOPOLOGY, IMG_ER, IMG_CICD]:
    print(p, "EXISTS:" if os.path.exists(p) else "MISSING!")
