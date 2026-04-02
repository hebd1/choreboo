# Design System Specification: Editorial Gamification

## 1. Overview & Creative North Star
**The Creative North Star: "The Living Narrative"**
Most habit trackers feel like spreadsheets with a coat of paint. This design system rejects the clinical nature of productivity tools in favor of a "Living Narrative." We are not just tracking chores; we are illustrating a journey. 

The system moves beyond the standard Material 3 "grid-of-cards" by utilizing **Dynamic Asymmetry** and **Tonal Depth**. By breaking the rigid 1:1 alignment of traditional Android apps, we create a sense of organic growth. Elements should feel like they are floating in a soft, pressurized environment—overlapping slightly to suggest a physical, tactile world where every completed task has "weight."

---

## 2. Colors & Surface Philosophy
The palette balances the energy of a RPG with the sophistication of a high-end editorial layout.

### Palette Strategy
- **Primary & Growth:** `primary` (#006e1c) and `primary_container` (#4caf50) represent the core "life force" of the app. Use these for progress and completion.
- **The Achievement Axis:** `secondary` (Vibrant Orange) is reserved strictly for high-value rewards (Stars/Points). `tertiary` (Deep Purple) is the "Lore" color, used exclusively for XP, Leveling, and long-term mastery.
- **Surface Hierarchy:** We utilize a "Depth-First" approach.
    - **Base:** `surface` (#f6fafe)
    - **Elevated Sections:** `surface_container_low` (#f0f4f8)
    - **Floating Interactive Elements:** `surface_container_highest` (#dfe3e7)

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined through:
1. **Background Shifts:** A `surface_container_low` section sitting on a `surface` background.
2. **Subtle Tonal Transitions:** Using the `surface_variant` to create a soft "pocket" for content.

### The "Glass & Gradient" Rule
To elevate the "game-ified" feel into a premium experience, use **Glassmorphism** for floating overlays (e.g., Level-Up modals). Apply a 20px `backdrop-blur` with a 60% opacity `surface_container_lowest`. 
*Signature Texture:* Main CTAs should use a linear gradient from `primary` to `primary_container` (top-left to bottom-right) to provide a "jewel-like" depth that flat hex codes lack.

---

## 3. Typography: Editorial Playfulness
We use **Plus Jakarta Sans** for its geometric clarity and friendly, open counters. 

- **Display & Headline (The 'Hero' Voice):** `display-lg` to `headline-sm` should be used with tight tracking (-2%) to feel impactful and modern. Use these for Streaks and Level numbers.
- **Title & Body (The 'Guide' Voice):** `title-md` and `body-lg` handle the instructional content. 
- **Labels (The 'Metadata' Voice):** `label-md` is used for XP values and timestamps.

The hierarchy is intentionally dramatic. A "Level 14" header (`display-lg`) should dwarf the surrounding body text to create a clear "Reward Center" for the user’s eyes.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too "heavy" for a playful system. Instead, we use **Tonal Layering**.

- **The Layering Principle:** Depth is achieved by "stacking." A `surface_container_lowest` card placed on a `surface_container_low` background creates a natural lift.
- **Ambient Shadows:** For "Floating Action Buttons" or critical modals, use an extra-diffused shadow: `blur: 24dp`, `opacity: 6%`, using the `on_surface` color tinted with `primary`.
- **The "Ghost Border" Fallback:** If a container requires more definition (e.g., on very high-brightness screens), use the `outline_variant` token at **15% opacity**. Never use 100% opaque outlines.

---

## 5. Component Guidelines

### Buttons (The "Tactile" Units)
- **Primary:** Gradient-filled (`primary` to `primary_container`), `1rem` (16dp) rounded corners. Use a subtle inner-glow (white at 10% opacity) on the top edge to simulate a "pressed" plastic feel.
- **Tertiary (Ghost):** No background, `primary` text. Use these for "Cancel" or "Skip" to keep the user focused on the positive path.

### Cards & Habit Lists
- **Structure:** Forbid the use of divider lines. 
- **Separation:** Use `3.5rem` (1.2) vertical white space or a shift to `surface_variant` for the card background.
- **The "Streak" Card:** Cards with active "Fire Streaks" should use an asymmetric layout—place the Emoji (🍖, 🥚) slightly overlapping the top-left edge of the card to break the "box" feel.

### Badges & Indicators
- **XP Badges:** Use `tertiary_container` with `on_tertiary_container` text. 
- **Status Indicators:** High-contrast circles. A "Missed Habit" shouldn't use a red border; it should use a `surface_dim` background to indicate "lost energy."

### Input Fields
- Use "Soft-Pocket" styling: A `surface_container_high` background with no border. On focus, transition the background to `surface_container_lowest` and add a `2px` `primary` "Ghost Border" at 40% opacity.

---

## 6. Do’s and Don’ts

### Do
- **Do** overlap emojis and icons over container edges to create a 3D effect.
- **Do** use `2rem` (lg) or `3rem` (xl) corner radii for large layout sections to maintain the "soft" brand identity.
- **Do** use the Spacing Scale strictly; asymmetry only works when the gaps between elements are intentional and consistent.

### Don't
- **Don't** use pure black (#000000) for text. Always use `on_surface` (#171c1f).
- **Don't** use standard Material 3 dividers. If you feel you need a line, use a wider gap of white space instead.
- **Don't** use more than one "Floating" shadow per screen. If everything floats, nothing is important.

---

## 7. Iconography & Emojis
Mix Material Symbols (Rounded) with high-detail Emojis. 
- **Functional Icons:** (Settings, Back, Edit) Use `outline` color tokens.
- **Emotional Icons:** (Rewards, Habits, Pets) Use full-color Emojis. 
*Rule:* Emojis must always be 25% larger than functional icons to signal their importance as "Game Objects."