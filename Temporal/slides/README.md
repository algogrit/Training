# Slides

Two [Marp](https://marp.app) decks for the Temporal training. Deployed to GitHub Pages as a single site.

| Deck | Audience | Duration | Hosted URL |
| --- | --- | --- | --- |
| [why-temporal/](why-temporal) | Engineers + their managers | ~25 min | <https://temporal-training.slides.codermana.com/why-temporal/> |
| [temporal-fundamentals/](temporal-fundamentals) | Engineers writing Workflow code | ~3 hrs | <https://temporal-training.slides.codermana.com/temporal-fundamentals/> |

Landing page (with links to both decks): <https://temporal-training.slides.codermana.com>

Both share the same theme tokens (orange/yellow accents on a dark background, Inter for text, JetBrains Mono for code). Each deck is otherwise self-contained — its own `slides.md`, `themes/base.css`, `package.json` — so you can extract a deck to its own repo later.

## Preview a deck

Each deck runs on its own port so you can preview both at once:

```bash
make slides-why            # :8080  (Why Temporal)
make slides-fundamentals   # :8081  (Temporal Fundamentals)
```

Generic form:

```bash
make slides DECK=why-temporal             PORT=8080
make slides DECK=temporal-fundamentals    PORT=8081
```

Or directly:

```bash
cd slides/why-temporal
npm install
PORT=8080 npm run preview
```

## Speaker notes

Both `slides.md` files carry inline presenter notes as HTML comments after
each slide. Press `p` in the Marp browser preview for presenter mode; the
notes also appear as speaker notes in the PPTX export (`npm run pptx`).

## Export

```bash
cd slides/why-temporal
npm run html       # dist/index.html
npm run pdf        # dist/slides.pdf
npm run pptx       # dist/slides.pptx
```

## Marp slide classes

Both decks use the layout classes defined in `themes/base.css`. Apply with Marp's `<!-- _class: ... -->` directive.

| Class | Use for |
| --- | --- |
| `title` | Cover slide |
| `section` | Section divider |
| `split` | Two-column content with a side visual |
| `speaker` | "About me" intro |
| `image` | Image-led slide |
| `quote` | Pull quote |
| `cards` | Three-column table summary |
| `code` | Code-focused slide |
| `exercise` | Workshop prompt |
| `takeaway` | Closing summary |

## New deck

Scaffold a new deck by copying one of the existing directories:

```bash
cp -R slides/why-temporal slides/new-deck-name
# edit slides.md and package.json's "name" field
```

The Pages workflow auto-detects any `slides/*/` directory with both `slides.md` and `package.json` and includes it in the build. After pushing, the new deck is reachable at `<site>/new-deck-name/`.

## Deployment

GitHub Pages, single site, sub-paths per deck.

- Workflow: [.github/workflows/pages.yml](../.github/workflows/pages.yml)
- Triggers: pushes to `master` touching `slides/**`, plus manual `workflow_dispatch`.
- Build: each `slides/*/` runs `npm ci && npm run html`. The resulting `dist/index.html` lands at `dist/<deck>/index.html` in the deploy artifact.
- CNAME: `slides/CNAME` → `temporal-training.slides.codermana.com`. Point that subdomain's DNS at GitHub Pages, then enable Pages with "GitHub Actions" as the source.
- Landing: `slides/index.html` becomes the root `index.html` of the deployed site.

## One-time GitHub setup

1. Push to `master`. The Pages workflow runs but the first deploy needs Pages enabled.
2. In the repo settings → **Pages** → set **Source** to **GitHub Actions**.
3. Add a `CNAME` record at your DNS provider:
   - `temporal-training.slides.codermana.com` → `<your-user>.github.io`
4. Re-run the workflow (`Actions` tab → `Deploy slides` → `Run workflow`).
5. Confirm the custom domain in **Settings → Pages**, and enable **Enforce HTTPS** once the cert provisions.

## Notes

- The template that seeded these decks lives separately at `slides-template/` (a standalone git repo). Treat it as the source of truth for the theme.
- Marp CLI requires Node 18+. `npm install` is per-deck.
- `slides/CNAME` is the single source of truth for the deployed domain. Per-deck `CNAME` files are intentionally absent — they would be ignored anyway in a sub-path deployment.
- Image alignment with `![bg right:38% w:88%](assets/images/example.jpg)` is the documented Marp pattern for visual split slides.
