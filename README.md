# CSE423 — Software Architecture

## Repository: Keycloak

**GitHub:** https://github.com/keycloak/keycloak

---

## Overview

So far, samples have been selected from the three categories (small, medium, large). Next, an LLM is used to generate a description for each selected Java file. The generated descriptions are then manually verified to ensure they accurately represent the corresponding source code.

---

## Procedure

1. Cloned the repository to my computer.
2. Opened Git Bash and navigated to the cloned repository directory.
3. The repository initially contained **8,215** Java files, obtained using:

   ```bash
   git ls-files "*.java" | wc -l
   ```

4. Listed the commits made before January 1, 2020:

   ```bash
   git log --before="2020-01-01"
   ```

5. Selected an appropriate commit from the filtered list and checked it out:

   ```bash
   git checkout 56d53b191a50deeecb782a1e4b723e906ad17b4f
   ```

6. After checking out this commit, the repository contained **4,990** Java files.
7. Sorted the Java files by lines of code (LOC) into three categories:

   | Category | LOC Range |
   |----------|-----------|
   | Small    | 0–50      |
   | Medium   | 51–99     |
   | Large    | ≥100      |

8. Applied the sampling formula to pick the required sample, resulting in **9, 10, and 11** samples for the small, medium, and large categories respectively.

---

## Sampling Procedure

### Population

| Group  | Population ($N_h$) |
|--------|---------------------|
| Small (≤50 lines)    | 1,439 |
| Medium (51–99 lines) | 1,596 |
| Large (≥100 lines)   | 1,955 |
| **Total**            | **4,990** |

### Step 1 — Calculate Total Sample Size (Cochran's Formula)

$$
n_0 = \frac{Z^2 pq}{e^2}
$$

Where:
- $Z = 1.645$ (90% confidence)
- $p = 0.5$
- $q = 0.5$
- $e = 0.15$

$$
n_0 = \frac{(1.645)^2 (0.5)(0.5)}{(0.15)^2} = 30.08
$$

### Step 2 — Apply Finite Population Correction

$$
n = \frac{n_0}{1 + \dfrac{n_0 - 1}{N}} = \frac{30.08}{1 + \dfrac{30.08 - 1}{4990}} = 29.9
$$

$$
\boxed{\text{Total sample} \approx 30 \text{ files}}
$$

### Step 3 — Allocate Samples Proportionally

$$
n_h = \frac{N_h}{N} \times n
$$

| Group  | Calculation | Sample |
|--------|-------------|--------|
| Small  | $\dfrac{1439}{4990} \times 30 = 8.65$  | ≈ 9  |
| Medium | $\dfrac{1596}{4990} \times 30 = 9.59$  | ≈ 10 |
| Large  | $\dfrac{1955}{4990} \times 30 = 11.74$ | ≈ 11 |

### Final Sample Allocation

| Segment              | Population | Sample |
|----------------------|-----------|--------|
| Small (≤50 lines)    | 1,439     | 9      |
| Medium (51–99 lines) | 1,596     | 10     |
| Large (≥100 lines)   | 1,955     | 11     |
| **Total**            | **4,990** | **30 files** |

This allocation preserves the original repository's size distribution:
- Small: 28.8%
- Medium: 32.0%
- Large: 39.2%

and avoids bias toward any single file-size category.

---

## Required Task: Dependency Structure Analysis

### Objective

Analyze how LLMs change system dependency structure.

### Steps

1. Extract dependency graph from the original system.
2. Reconstruct the system using an LLM.
3. Build a dependency graph for the LLM output.
4. Compare both graphs.

### Metrics

- Graph Edit Distance (GED)
- Cycle Count (CC)
- Centralization Shift Score (CSS)
