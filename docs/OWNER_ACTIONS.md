# OWNER_ACTIONS — latebris

GitHub API cannot upload repository social previews. Manual steps required.

## Social preview — manual upload

1. Go to `https://github.com/ianlyoo/latebris/settings` → `General` → `Social preview` → `Edit` → `Upload an image`.
2. Upload file `docs/assets/social-preview.png` (1280×640, <1MiB, deterministic sharp PNG with compressionLevel 9).
3. Save.

**Verification queries:**

```bash
curl -fsSL https://ianlyoo.github.io/latebris/ | grep -o 'og:image[^>]*content="[^"]*"'
curl -fsSL https://ianlyoo.github.io/latebris/assets/social-preview.png -o /tmp/p.png && ls -l /tmp/p.png
gh api graphql -f query='query{repository(owner:"ianlyoo",name:"latebris"){openGraphImageUrl}}'
gh repo view ianlyoo/latebris --json description,homepageUrl
gh api repos/ianlyoo/latebris/topics --jq '.names | sort'
```

**Determinism check:**

```bash
sha256sum docs/assets/social-preview.png
```

**Note:** API upload prohibited — use manual Settings path only.
