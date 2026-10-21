"""L'indice dell'archivio di PLANNING (21 ott 2026).

Il piano vive in `PLANNING.md` (stato, aperti, registro del periodo corrente) e i periodi
chiusi in `docs/planning/NN-<periodo>.md`, congelati. Questo script scrive
`docs/planning/README.md`: ogni titolo `##` e `###` di ogni file d'archivio, con la riga, così
un rimando per nome («PLANNING, Fase 11», «Il motore degli stati») si risolve con una ricerca
sull'indice e una lettura dalla riga giusta.

Il registro vivo non ci sta: è corto e si cerca da sé, e l'indice cambia così solo quando si
archivia (il registro di `PLANNING.md` passa in un nuovo file d'archivio). Allora si rilancia:

    python tools/planning_index.py           # riscrive l'indice
    python tools/planning_index.py --check   # esce con 1 se l'indice non è al passo
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ARCHIVE = ROOT / "docs" / "planning"
INDEX = ARCHIVE / "README.md"

HEADER = """# L'archivio di PLANNING

I periodi chiusi del registro di Chiaro, uno per file, **congelati**: non si modificano. Il
piano vivo (stato, aperti, registro del periodo corrente) è [`PLANNING.md`](../../PLANNING.md),
che dice anche come si archivia. Una decisione che cambia una di queste si scrive lì, con il
rimando a quella che cambia.

Questo indice è scritto da `tools/planning_index.py`: non si modifica a mano. Ogni titolo di
sezione (`##`) e di sottosezione (`###`) dell'archivio con la sua riga, per cercare un rimando
per nome e leggere dal punto giusto. Il registro del periodo corrente non c'è: è in
`PLANNING.md`, corto, e si cerca lì.
"""

HEADING = re.compile(r"^(#{2,3}) (.+?)\s*$")


def files() -> list[pathlib.Path]:
    return sorted(ARCHIVE.glob("[0-9][0-9]-*.md"))


def title(path: pathlib.Path, lines: list[str]) -> str:
    for line in lines:
        if line.startswith("# "):
            return line[2:].strip()
    return path.name


def render() -> str:
    out = [HEADER]
    for path in files():
        lines = path.read_text(encoding="utf-8").splitlines()
        out.append(f"\n## [{path.name}]({path.name}): {title(path, lines)}\n")
        fence = False
        for number, line in enumerate(lines, start=1):
            if line.startswith("```"):
                fence = not fence
            if fence:
                continue
            match = HEADING.match(line)
            if not match:
                continue
            indent = "" if len(match.group(1)) == 2 else "  "
            out.append(f"{indent}- L{number}: {match.group(2)}")
    return "\n".join(out) + "\n"


def main() -> int:
    text = render()
    if "--check" in sys.argv:
        current = INDEX.read_text(encoding="utf-8") if INDEX.exists() else ""
        if current != text:
            print("docs/planning/README.md non è al passo: python tools/planning_index.py")
            return 1
        return 0
    INDEX.write_text(text, encoding="utf-8", newline="\n")
    print(f"{INDEX.relative_to(ROOT).as_posix()}: {text.count(chr(10) + '- L') + text.count(chr(10) + '  - L')} titoli")
    return 0


if __name__ == "__main__":
    sys.exit(main())
