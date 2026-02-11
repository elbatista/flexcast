# FlexCast

FlexCast is a **genuine overlay-based atomic multicast protocol** designed to improve communication efficiency and exploit locality in scalable State Machine Replication (SMR) systems.

The core idea behind FlexCast is to ensure that **only the sender and the destination groups participate in ordering a multicast message**, using a **complete directed acyclic graph (C-DAG)** overlay to restrict communication while preserving atomicity and consistency.

This repository contains the research prototype used in the experimental evaluation of FlexCast.

---

## Key Features

- Genuine atomic multicast using an overlay  
- Complete DAG (C-DAG) overlay design  
- Quiescent communication: uninvolved replicas stop exchanging messages  
- Exploits workload locality to reduce wide-area communication  
- Online reconfiguration to adapt to workload changes  

---

## Background

FlexCast was developed as part of research on scalable, dependable distributed systems.  
It addresses the inefficiency of traditional atomic broadcast and non-genuine multicast protocols by combining genuineness and constrained connectivity, leading to lower latencies in geographically distributed deployments.

---

## Repository Status

**Research prototype only**

- Intended for experimentation and reproducibility  
- Not production-hardened  
- APIs and implementation details are subject to change

---

## Publication

If you use or refer to this code, please cite the following paper:

> **Eliã Batista, Paulo Coelho, Eduardo Alchieri, Fernando Dotti, and Fernando Pedone.**  
> *FlexCast: Genuine Overlay-based Atomic Multicast*.  
> In *Proceedings of the 24th International Middleware Conference (Middleware ’23)*, December 2023.  
> ACM. doi:10.1145/3590140.3629122

---

## License

This project is released under the **MIT License** (or update as appropriate).
