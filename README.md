<div align="center">

# 🔍 Dependency Structure Analysis
### LLM-Generated Code vs. Human-Evolved Code

**CSE423 — Software Engineering Structure Analysis**

📦 **Repository under study:** [`keycloak/keycloak`](https://github.com/keycloak/keycloak)
🎯 **Task:** Dependency Structure Analysis
🧪 **Sample:** 30 Java files — `9 Small` · `10 Medium` · `11 Large`

![Java](https://img.shields.io/badge/Language-Java-orange)
![Files Analyzed](https://img.shields.io/badge/Files%20Analyzed-30-blue)
![Repo](https://img.shields.io/badge/Subject%20Repo-Keycloak-4B275F)
![Status](https://img.shields.io/badge/Status-Complete-brightgreen)

</div>

---

## 📑 Table of Contents

1. [Repository Selection & Justification](#1-repository-selection--justification)
2. [Pre-LLM System Snapshot & Task Analysis](#2-pre-llm-system-snapshot--task-analysis)
3. [LLM Prompt Design & Iterative Refinement](#3-llm-prompt-design--iterative-refinement)
4. [LLM-Generated Code Quality](#4-llm-generated-code-quality)
5. [Metric Calculation & Accuracy](#5-metric-calculation--accuracy)
6. [Comparative Analysis & Reflection](#6-comparative-analysis--reflection)
7. [Limitations](#7-limitations)
8. [Final Submission Checklist & Artifact Index](#8-final-submission-checklist--artifact-index)

---

## 1. Repository Selection & Justification

The repository chosen for this analysis is **Keycloak**, an open-source identity and access management platform maintained by Red Hat — [`github.com/keycloak/keycloak`](https://github.com/keycloak/keycloak).

### 1.1 Selection Rule Evidence

| Rule | Requirement | Evidence |
|---|---|---|
| **Language** | Python / Java / TypeScript | ✅ Java — Keycloak is a Java/Spring-ecosystem (WildFly/Quarkus) IAM system |
| **Size** | ≥10,000 LOC and ≥500 commits | ✅ 8,215 Java files at HEAD; 4,990 remained at the selected pre-2020 commit *(commit-count threshold not independently re-verified — see [§7](#7-limitations))* |
| **History** | First commit <2020, ≥2 yrs active dev | ✅ Snapshot commit `56d53b1` selected pre-2020; Keycloak's first release was 2014 |
| **Structure** | ≥2 of controller/service/model/repository/src/core/utils | ✅ `src/main/java`, multi-module Maven layout with `core/`, `model/`, services-style packages |
| **System Type** | Web backend / API / framework-based | ✅ Full IAM server — REST APIs, admin console, SPI-based extensibility |

### 1.2 Selection Procedure

```bash
# Clone & count Java files at HEAD
git ls-files "*.java" | wc -l          # → 8,215 files

# Filter to pre-2020 commit history
git log --before="2020-01-01"

# Checked out snapshot commit
56d53b191a50deeecb782a1e4b723e906ad17b4f

# Re-count at that commit → sampling population
4,990 Java files
```

> 💡 *"Not Allowed" criteria — toy projects, single-file projects, tutorial repos, notebook-only projects — don't apply. Keycloak is a large, real-world, actively maintained IAM platform.*

---

## 2. Pre-LLM System Snapshot & Task Analysis

**Snapshot commit:** `56d53b191a50deeecb782a1e4b723e906ad17b4f` — a verifiable pre-LLM-era snapshot, predating general LLM code-generation availability.

### 2.2 Sampling Methodology

A stratified sample of 30 files was drawn from the 4,990-file population using **Cochran's formula**, then proportionally allocated across three LOC-based strata.

| Group | LOC Range | Population | % of Total | Sample |
|---|:---:|:---:|:---:|:---:|
| Small | 0–50 | 1,439 | 28.8% | **9** |
| Medium | 51–99 | 1,596 | 32.0% | **10** |
| Large | ≥100 | 1,955 | 39.2% | **11** |
| **Total** | — | **4,990** | **100%** | **30** |

> n₀ = Z²pq/e² = (1.645)²(0.5)(0.5)/(0.15)² = 30.08 → finite-population-corrected ≈ 29.9 → **30 files**

### 2.3 Structural Observations — Human Code

Every one of the 30 human-authored files exhibits a **single-hub star topology**: one center node (the file) with directed edges outward to each dependency — no leaf-to-leaf edges, no cycles. Two files (`KeyUse`, `DeviceTypeType`) are isolated 0-dependency enum nodes.

<div align="center">
<img src="imgs/c40ff50bd582d8e56c9d398eebd709b78dbd798d.png" width="420" alt="Human dependency graph — OIDCLoginProtocolFactory"/>

*Figure 2.1 — Human graph, `OIDCLoginProtocolFactory.java` (Large, fan-out = 36)*

<img src="imgs/713c2ff37a6edfd1b0e730894ecf5d6a0de7327a.png" width="360" alt="Human dependency graph — Autheticator"/>

*Figure 2.2 — Human graph, `Autheticator.java` (Small, fan-out = 2)*
</div>

---

## 3. LLM Prompt Design & Iterative Refinement

Three prompt iterations were used to reconstruct each sampled file from its JSON blueprint, each fixing a specific weakness observed in the previous version.

| Version | Fixes | Still Has |
|---|---|---|
| **v1 — Naive Baseline** | *(baseline)* | No structural guarantees; import drift, hollow bodies, invented classes |
| **v2 — Structural Constraints** | Exact naming, full import coverage, real method bodies enforced | Occasional stray import or thin method body |
| **v3 — Self-Verifying (Final)** ⭐ | Pre-build checklist + mandatory self-audit before output | Residual risk only if blueprint itself is incomplete |

<details>
<summary><b>🔽 Prompt v3 — Final (click to expand)</b></summary>

```text
You are reconstructing a Java class from a structured JSON blueprint, for a controlled
software-structure-analysis experiment. The generated file's dependency graph (imports,
class relationships, method call graph) will be directly diffed against the original
file's dependency graph, so EXACT structural fidelity to the blueprint is the top priority.
Functional behavior is secondary and may differ from any real implementation.

BLUEPRINT:
{PASTE_ONE_JSON_OBJECT_HERE}

STEP 1 - Build a checklist (silently reason it out):
a. List every item in imports[].import.
b. List every item in dependency.project_classes and dependency.external_libraries.
c. List every method key in dependency.method_call_graph, with its required calls.
d. List every entry in field_relationships and constructor_relationships.
e. List inheritance.extends, inheritance.interfaces, and annotations.

STEP 2 - Write the Java file, satisfying every checklist item exactly.

STEP 3 - Self-audit: re-read the generated code against the checklist, item by item,
and fix any mismatch before finalizing.

OUTPUT FORMAT - exactly two sections:
=== VERIFICATION CHECKLIST ===
=== FINAL JAVA CODE ===
```

</details>

Only the **`FINAL JAVA CODE`** section of v3 was used as the submitted `.java` file for each of the 30 files; the verification checklist doubled as a review flag for any file with a ❌.

---

## 4. LLM-Generated Code Quality

All 30 LLM-reconstructed files match the human sample **1:1 by filename and category**, and are complete, structurally non-trivial Java files — not stubs or pseudo-code.

| Category | Files | Approx. Human LOC | Notes |
|---|:---:|---|---|
| Small | 9 | ~991–1,604 bytes/file | Simple POJOs/interfaces/enums — perfect structural fidelity |
| Medium | 10 | ~1,624–4,310 bytes/file | Mix of test classes, SPI definitions, model types |
| Large | 11 | ~2,682–20,288 bytes/file | Multi-dependency service/bean/test classes — steepest drift |

---

## 5. Metric Calculation & Accuracy

### 5.1 Methodology

Every dependency graph — human and LLM alike — is a single-hub star topology, giving four structural guarantees:

| # | Guarantee | Reasoning |
|:-:|---|---|
| 1 | `\|M\| = fan-out + 1` | file itself + one node per dependency |
| 2 | `\|E\| = fan-out` | one outgoing edge per dependency |
| 3 | `CC(G) = 0`, always | no closed path possible in a star graph |
| 4 | `Cmax(G) = fan-out` (raw) / `1.0` (normalized) | hub is always most-connected by construction |

- **GED cost model:** identity match = 0; leaf present in only one graph = 2 (node + edge)
- **CSS** = `fanout_L − fanout_H` (raw fan-out, since normalized Cmax = 1.0 for both, always)

### 5.2 Per-File Metrics (H vs. L)

<details>
<summary><b>📊 Click to expand full 30-file table</b></summary>

| Category | File | \|M_H\| | fanout_H | \|M_L\| | fanout_L | GED | CSS |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Small | Autheticator | 3 | 2 | 3 | 2 | 0 | 0 |
| Small | ClientAuthUtil | 4 | 3 | 4 | 3 | 0 | 0 |
| Small | KeyUse | 1 | 0 | 1 | 0 | 0 | 0 |
| Small | KeycloakTransactionCommitter | 7 | 6 | 7 | 6 | 0 | 0 |
| Small | MissingAssertionSig | 5 | 4 | 5 | 4 | 0 | 0 |
| Small | PermissionTicketListQuery | 5 | 4 | 5 | 4 | 0 | 0 |
| Small | Spi | 3 | 2 | 3 | 2 | 0 | 0 |
| Small | TimerProvider | 3 | 2 | 3 | 2 | 0 | 0 |
| Small | UndertowAppServerArquillianExtension | 7 | 6 | 7 | 6 | 0 | 0 |
| Medium | DeviceTypeType | 1 | 0 | 1 | 0 | 0 | 0 |
| Medium | IdpConfirmLinkAuthenticator | 17 | 16 | 17 | 16 | 0 | 0 |
| Medium | ImportTest | 17 | 16 | 13 | 12 | 12 | **-4** |
| Medium | KeyStoreDefinition | 8 | 7 | 7 | 6 | 2 | **-1** |
| Medium | LDAPDnTest | 4 | 3 | 4 | 3 | 0 | 0 |
| Medium | LoggedInPageHeader | 5 | 4 | 5 | 4 | 4 | 0 |
| Medium | MediumType | 4 | 3 | 4 | 3 | 0 | 0 |
| Medium | RealmResourceSPI | 6 | 5 | 6 | 5 | 0 | 0 |
| Medium | SAML11AuthorizationDecisionQueryType | 8 | 7 | 4 | 3 | 8 | **-4** |
| Medium | StringSerializationTest | 8 | 7 | 4 | 3 | 8 | **-4** |
| Large | AbstractClientRegistrationTest | 17 | 16 | 11 | 10 | 12 | **-6** |
| Large | ApplicationsBean | 22 | 21 | 16 | 15 | 12 | **-6** |
| Large | AuthorizationBean | 25 | 24 | 15 | 14 | 20 | **-10** |
| Large | GetRolesCmd | 22 | 21 | 11 | 10 | 38 | **-11** |
| Large | JWEKeyStorage | 6 | 5 | 3 | 2 | 6 | **-3** |
| Large | LDAPObject | 10 | 9 | 2 | 1 | 16 | **-8** |
| Large | OIDCLoginProtocolFactory | 37 | 36 | 31 | 30 | 16 | **-6** |
| Large | PersonalInfoTest | 12 | 11 | 8 | 7 | 12 | **-4** |
| Large | ThemeResourceDefinition | 19 | 18 | 5 | 4 | 28 | **-14** |
| Large | UserCredentialStoreManager | 30 | 29 | 21 | 20 | 22 | **-9** |
| Large | UserUpdateProfileContext | 6 | 5 | 4 | 3 | 4 | **-2** |

*`CC(G_H) = CC(G_L) = 0` for all 30 files in both datasets (omitted as a column — constant by construction).*

</details>

### 5.3 Aggregate Summary

| Category | Σfanout_H | Σfanout_L | Δ fanout | % change | Σ GED | Σ CSS |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| Small (9) | 29 | 29 | 0 | 0.0% | 0 | 0 |
| Medium (10) | 68 | 55 | -13 | -19.1% | 34 | -13 |
| Large (11) | 195 | 116 | **-79** | **-40.5%** | 186 | -79 |
| **Total (30)** | **292** | **200** | **-92** | **-31.5%** | **220** | **-92** |

### 5.4 Visual Evidence

<div align="center">
<img src="imgs/b110494b77c848369d0bd0fa143eaecec15d8d7e.png" width="420" alt="LLM dependency graph — OIDCLoginProtocolFactory"/>

*Figure 5.1 — LLM-reconstructed graph, `OIDCLoginProtocolFactory.java` (fan-out = 30 vs. 36 human)*

<img src="imgs/f356c8b615a90288e8f932ee8dc0b7b79df45573.png" width="360" alt="LLM dependency graph — Autheticator"/>

*Figure 5.2 — LLM-reconstructed graph, `Autheticator.java` (fan-out = 2, identical to human)*
</div>

---

## 6. Comparative Analysis & Reflection

> ### 🔑 Key Finding
> **The LLM decentralizes — it does not centralize.** Every non-zero CSS value across all 30 files is **negative**. This is the opposite of the commonly-assumed "God class" failure mode for LLM-generated code.

- **Cycle Count** is uninformative by construction — `CC = 0` for every file, human and LLM alike, since a star graph structurally cannot form a cycle.
- **GED scales with complexity** — total GED = 220, driven almost entirely by *dropped* leaves (101 dropped vs. only 9 added). All 9 Small files match perfectly (GED = 0); Large files account for 186 of the 220 total edits.
- **What gets dropped:** mostly JDK collection/utility imports (`List`, `Set`, `Map`, `HashMap`, `Collections`...) and JUnit/Hamcrest test scaffolding (`Before`, `Test`, `assertTrue`...) — domain-specific business logic is dropped far less often.
- **Structural insight:** the LLM reconstructs a file's *architectural intent* faithfully, but systematically under-reconstructs its "plumbing" — standard-library imports and test-framework wiring — unless explicitly forced to include them.

---

## 7. Limitations

- **File-local scope** — GED, CC, and CSS are computed per-file, not on a merged whole-codebase call graph; can't detect cross-file reorganization or inheritance-depth shifts.
- **GED cost model** is a stated convention (2 edits/mismatched leaf), not fixed by the assignment's Appendix.
- **Normalized centrality** isn't a useful discriminator here — it's `1.0` for every non-isolated file in both datasets by construction.
- **Repository-selection sub-evidence** — exact commit count (≥500) and an explicit folder listing at the checkout commit were not independently re-confirmed.
- **Blueprint dependency** — LLM fidelity is bounded by how complete the input JSON blueprint is; blueprint gaps could be misread as LLM structural failure.

---

## 8. Final Submission Checklist & Artifact Index

| Requirement | Status | Location |
|---|:---:|---|
| Selected GitHub repository (link) | ✅ | §1.1 |
| Pre-LLM system snapshot | ✅ | §2, `Human_Java_Files_Graph.zip` |
| LLM prompt used (with iterations) | ✅ | §3 |
| LLM-generated code | ✅ | `LLM_Generated_Java_Files.zip` (30 files) |
| Analysis report | ✅ | §6 |
| Metric calculations (H and L) | ✅ | §5 |
| Final comparison summary | ✅ | §6 |

**📦 Bundled Artifacts**

```
├── Human_Java_Files.zip                 # 30 human-authored .java files
├── LLM_Generated_Java_Files.zip         # 30 LLM-reconstructed .java files
├── Human_Java_Files_Graph.zip           # 30 dependency-graph PNGs (human)
├── LLM_Generated_Java_Files_Graph.zip   # 30 dependency-graph PNGs (LLM)
├── Small_Descriptions.json              # JSON blueprints — Small (9)
├── Medium_Descriptions.json             # JSON blueprints — Medium (10)
├── Large_Descriptions.json              # JSON blueprints — Large (11)
└── Structured_Metrics_Report.md         # Full per-file metrics & methodology
```

<div align="center">

---

*Built as part of CSE423 — Software Engineering Structure Analysis*

</div>
