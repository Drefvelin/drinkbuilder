Place vanilla-style potion textures here (copied to plugin data/assets on first enable):

  potion_overlay.png  — liquid layer (tinted by drink color / CustomPotionColor)
  glass_bottle.png    — glass bottle layer (not tinted)

On enable / reload / catalog sync, DrinkBuilder uploads both PNGs to ProvinceSystem
(PUT /drinks/plugin/assets/…). The website and Discord review sheets load them from
the API — do not copy into ProvinceSystem or the frontend repo.
