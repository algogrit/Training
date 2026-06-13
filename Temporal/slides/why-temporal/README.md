# Why Temporal — slide deck

Persuasive intro deck. ~30 slides, ~25 minutes. Built with [Marp](https://marp.app).

## Preview

```bash
npm install                  # first time only
PORT=8080 npm run preview    # http://localhost:8080
```

From the repo root:

```bash
make slides-why              # runs on :8080
```

## Speaker notes

`slides.md` carries inline presenter notes as HTML comments after each slide.
Marp preserves them as speaker notes in PPTX exports and shows them in
presenter mode in the browser (press `p`).

## Export

```bash
npm run html       # dist/index.html
npm run pdf        # dist/slides.pdf
npm run pptx       # dist/slides.pptx
```

## Edit

- Content: `slides.md`
- Theme: `themes/base.css`
- Images: `assets/images/`

The starter slide patterns (title, section, split, speaker, cards, quote, code, exercise, takeaway) are documented at the root `slides/README.md`.

## Deploy

CNAME ships set to `why-temporal.slides.codermana.com`. Adjust as needed.
