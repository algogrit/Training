# Temporal Fundamentals — slide deck

Technical companion deck for the training. ~60 slides, ~3 hours including demos. Built with [Marp](https://marp.app).

## Preview

```bash
npm install                  # first time only
PORT=8081 npm run preview    # http://localhost:8081
```

From the repo root:

```bash
make slides-fundamentals     # runs on :8081
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

## Deploy

CNAME ships set to `temporal-fundamentals.slides.codermana.com`. Adjust as needed.
