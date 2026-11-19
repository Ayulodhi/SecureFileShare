# -*- coding: utf-8 -*-
"""
report_sections.py
Contains all text, table definitions, figure captions, and technical explanations
for the SecureFileShare MCA Project Report.
"""

PRELIMINARIES = {
    "project_title": "SecureFileShare – Zero-Knowledge Encrypted File Sharing & Vault Platform",
    "degree": "MASTER OF COMPUTER APPLICATIONS",
    "batch": "Batch (2025–2027)",
    "candidate_name": "Ayush Lodhi",
    "enrolment_no": "GE - 25113399",
    "guide_name": "Dr. Project Supervisor / Faculty Guide",
    "guide_title": "Professor / Associate Professor",
    "dept_name": "Department of Computer Applications",
    "univ_name": "Graphic Era (Deemed to be University)",
    "univ_location": "Dehradun, Uttarakhand",
    "submission_date": "November-2026"
}

ABBREVIATIONS = [
    ("AES", "Advanced Encryption Standard", "Ch. 1"),
    ("GCM", "Galois/Counter Mode", "Ch. 1"),
    ("RSA", "Rivest–Shamir–Adleman Asymmetric Cryptosystem", "Ch. 1"),
    ("OAEP", "Optimal Asymmetric Encryption Padding", "Ch. 1"),
    ("ECDSA", "Elliptic Curve Digital Signature Algorithm", "Ch. 1"),
    ("SHA", "Secure Hash Algorithm", "Ch. 1"),
    ("NIST", "National Institute of Standards and Technology", "Ch. 1"),
    ("JWT", "JSON Web Token", "Ch. 1"),
    ("API", "Application Programming Interface", "Ch. 1"),
    ("REST", "Representational State Transfer", "Ch. 1"),
    ("DFD", "Data Flow Diagram", "Ch. 2"),
    ("SRS", "Software Requirements Specification", "Ch. 2"),
    ("UML", "Unified Modeling Language", "Ch. 2"),
    ("ER", "Entity Relationship", "Ch. 3"),
    ("PK", "Primary Key", "Ch. 3"),
    ("FK", "Foreign Key", "Ch. 3"),
    ("ORM", "Object Relational Mapping", "Ch. 3"),
    ("JPA", "Java Persistence API", "Ch. 3"),
    ("UI", "User Interface", "Ch. 3"),
    ("UX", "User Experience", "Ch. 3"),
    ("WBS", "Work Breakdown Structure", "Ch. 4"),
    ("PERT", "Program Evaluation and Review Technique", "Ch. 4"),
    ("CI/CD", "Continuous Integration / Continuous Deployment", "Ch. 4"),
    ("GHCR", "GitHub Container Registry", "Ch. 4"),
    ("CSP", "Content Security Policy", "Ch. 7"),
    ("CORS", "Cross-Origin Resource Sharing", "Ch. 7"),
    ("RBAC", "Role-Based Access Control", "Ch. 7"),
    ("OWASP", "Open Worldwide Application Security Project", "References")
]

LIST_OF_FIGURES = [
    ("Figure 2.1", "Use Case Diagram of SecureFileShare Platform", "22"),
    ("Figure 2.2", "Sequence Diagram of Recipient Ciphertext Upload & Key Sharing Pipeline", "26"),
    ("Figure 2.3", "Activity Diagram: Encrypted File Storage & Retrieval Flow", "28"),
    ("Figure 3.1", "Client-Service Monorepo Architecture of SecureFileShare", "32"),
    ("Figure 3.2", "Detailed Microservice Component & Storage Topology", "34"),
    ("Figure 3.3", "Database Entity Relationship (ER) Diagram", "37"),
    ("Figure 4.1", "Continuous Integration and Continuous Deployment (CI/CD) Pipeline", "43")
]

LIST_OF_TABLES = [
    ("Table 2.1", "Functional Requirements of SecureFileShare", "20"),
    ("Table 2.2", "Non-Functional Requirements of SecureFileShare", "21"),
    ("Table 3.1", "Communication Flow Between System Layers of SecureFileShare", "33"),
    ("Table 3.2", "Entities and Attributes of SecureFileShare System", "36"),
    ("Table 3.3", "Database Tables and Structure Specifications", "38"),
    ("Table 3.4", "User Interface Components and Description", "46"),
    ("Table 4.1", "Work Breakdown Structure (WBS)", "58"),
    ("Table 4.2", "Project Timeline (8-Week Lifecycle)", "59"),
    ("Table 4.3", "Project Milestones & Target Deliverables", "61"),
    ("Table 4.4", "Phase-wise Project Deliverables", "62"),
    ("Table 4.5", "Risk Analysis & Impact Matrix", "64"),
    ("Table 4.6", "Cost Estimation of the Project", "67"),
    ("Table 6.1", "Sample Encrypted Vault File Records (Table View)", "83"),
    ("Table 6.2", "Sample Audit Trail & Hash-Chain Verification Log", "85"),
    ("Table 7.1", "Unit Testing Details & Validation Results", "88"),
    ("Table 7.2", "System Test Cases & Security Validation Results", "93"),
    ("Table 7.3", "User Roles and Access Permissions Matrix", "98")
]
