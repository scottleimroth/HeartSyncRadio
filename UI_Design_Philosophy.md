# UI Design Philosophy

> "Simplicity is the ultimate sophistication." — Leonardo da Vinci
>
> This document is your design conscience. Drop it into any repo. Point any AI agent at it. Reference it in code reviews. It covers the full spectrum of design thinking — from pixel-level precision to the feeling a user walks away with.

---

## How to Use This Document

**For humans:** Read it once fully, then reference specific sections during design and review.

**For AI agents:** Ingest this file at the start of any design session. It replaces ad-hoc design instructions with a consistent philosophy. Pair it with your project's `DESIGN_SYSTEM.md` (tokens), `APP_FLOW.md` (routes/journeys), and `PRD.md` (requirements) for full context.

**For code reviews:** Use the checklists in each section as review criteria. If a PR introduces UI changes, it should pass the relevant sections here.

---

## Part 1: Core Philosophy

### The Fundamental Rule

If a user needs to think about how to use it, you've failed. If an element can be removed without losing meaning, it must be removed. The best interface is the one the user never notices — like a well-designed door handle, you just grab it and pull.

### The Five Pillars

**1. Simplicity is architecture, not style.**
Simplicity isn't "make it minimal." It's the structural decision to solve the problem with the fewest possible elements. Think of it like engineering a bridge — you don't remove steel beams to make it look clean, you design it so every beam does maximum work. A cluttered interface is a structural failure.

**2. Every pixel must earn its place.**
For every element on screen, ask: "Can I remove this without losing meaning?" If yes, remove it. This isn't about being sparse — it's about being intentional. A well-set dinner table has everything you need and nothing you don't.

**3. Design is how it works, not how it looks.**
A beautiful button that confuses people is bad design. An ugly button that everyone understands is closer to good design. The goal is both — something that works so intuitively it *becomes* beautiful through its clarity.

**4. Consistency builds trust.**
When a button looks different on two screens, the user's brain burns energy figuring out if they're the same thing. Consistency is what lets users build a mental model of your app. It's like driving — you trust that the brake pedal works the same way in every car.

**5. The details the user never sees matter.**
The back of the cabinet should be finished as well as the front. Clean code, consistent naming, proper spacing in places users rarely visit — these compound into the overall feeling of quality. Users can't point to it, but they feel it.

---

## Part 2: Visual Design Principles

### Hierarchy — Direct the Eye

Every screen has a story. Hierarchy is how you tell it.

**The 2-Second Test:** Can a new user understand what this screen is about and what to do next within 2 seconds? If not, your hierarchy is broken.

**Rules:**
- Every screen has ONE primary action. Make it unmissable. Think of a stop sign — there's no ambiguity about what it wants you to do.
- Secondary actions support; they never compete. They're the fine print beneath the headline.
- If everything is bold, nothing is bold. Visual weight must match functional importance.
- Size, colour, contrast, and position all work together to create hierarchy. Don't rely on just one.

**Common failures:**
- Multiple CTAs (call-to-action buttons) competing for attention at the same visual weight
- Important information buried below the fold or in low-contrast text
- Navigation that draws more attention than the content it serves

### Typography — The Voice of Your Interface

Type is the primary way your app communicates. Get it wrong and everything feels off — like a restaurant with a handwritten menu in Comic Sans.

**Rules:**
- Establish a clear type scale: no more than 4–5 distinct sizes across the entire app.
- Use weight (bold, medium, regular) to create sub-hierarchy within a size. Don't use more than 3 weights.
- Line height matters more than you think. Body text needs 1.4–1.6× line height. Headings need tighter (1.1–1.3×).
- Limit line length to 50–75 characters for readability. Walls of text that stretch edge-to-edge are exhausting.
- One typeface family is almost always enough. Two is the maximum. Three is chaos.

**Common failures:**
- Too many font sizes creating a "ransom note" effect
- Insufficient contrast between heading levels
- Body text too small on mobile (minimum 16px for body)
- Mixing typefaces without a clear reason

### Colour — Use It Like a Scalpel, Not a Paintbrush

Colour should guide attention, convey meaning, and create mood — nothing more.

**Rules:**
- Start with a neutral palette (greys, whites, near-blacks). Add colour only where it serves a purpose.
- One primary brand colour. One or two accent colours. That's it.
- Colour must never be the *only* way to convey information (accessibility). Pair it with icons, text, or patterns.
- Maintain WCAG AA contrast ratios minimum: 4.5:1 for body text, 3:1 for large text and UI components.
- Dark mode isn't "invert everything." It requires its own considered palette where contrast ratios hold up and surfaces create depth through subtle lightness shifts.

**Common failures:**
- Using colour for decoration rather than communication
- Insufficient contrast making text hard to read
- Status colours (red/green) without accompanying text or icons
- Dark mode as an afterthought with broken contrast

### Spacing & Whitespace — The Invisible Architecture

Whitespace isn't empty space. It's structure. It's what separates "premium" from "cramped."

**Rules:**
- Use a consistent spacing scale (e.g., 4, 8, 12, 16, 24, 32, 48, 64). Every margin and padding should come from this scale — like musical notes from a scale rather than random frequencies.
- Related items sit closer together; unrelated items sit further apart (Gestalt proximity). This is how the eye groups things without needing borders or labels.
- When in doubt, add more space, not more elements. Breathing room feels premium.
- Maintain consistent vertical rhythm — the invisible grid that makes a page feel harmonious even when you can't explain why.

**Common failures:**
- Inconsistent spacing that feels arbitrary
- Elements crammed together, making the interface feel cheap
- Using borders and dividers where whitespace alone would create separation
- Different padding values for the same type of component

### Alignment & Grid — Precision Is Premium

The eye detects misalignment before the brain can name it. It's the design equivalent of a slightly crooked picture frame — you just know something's off.

**Rules:**
- Every element sits on a grid. No exceptions.
- If something is off by 1–2 pixels, it's wrong. Audit at zoom.
- Use an 8px grid as your foundation. Most spacing, sizing, and positioning should snap to multiples of 8.
- Left-align by default. Centre-align sparingly and with purpose (headings, hero sections). Right-align almost never for body content.

**Common failures:**
- Elements that are "close enough" to aligned but not actually aligned
- Mixed alignment patterns on the same screen
- Components that don't sit on the grid, creating visual tension
- Text and icons not vertically centred within their containers

### Iconography — Support Meaning, Don't Decorate

Icons should be read, not admired.

**Rules:**
- Use one cohesive icon set across the entire app. Mixing icon libraries (some outlined, some filled, some with rounded corners, some sharp) is like mixing fonts — it looks amateur.
- Icons supplement text; they rarely replace it. A floppy disk icon for "save" only works if your users have seen a floppy disk.
- Maintain consistent size, stroke weight, and optical weight across all icons.
- Interactive icons need sufficient touch/click targets (minimum 44×44px on touch devices).

**Common failures:**
- Icons from mixed libraries with different visual styles
- Icons used without labels where meaning is ambiguous
- Inconsistent icon sizes across the interface
- Decorative icons that add visual noise without aiding comprehension

---

## Part 3: Interaction & Behaviour

### Navigation — The Skeleton of the Experience

Navigation is the roadmap of your app. Bad navigation is like a building with no signs — technically usable, but hostile.

**Rules:**
- Users should always know where they are, where they can go, and how to get back. These three questions should be answerable at a glance.
- Primary navigation is persistent and consistent across the entire app.
- Depth creates confusion. Prefer flat structures. If a user is more than 3 taps/clicks deep, reconsider the information architecture.
- Mobile navigation must be reachable by thumb. Bottom navigation or hamburger menus that expand from the bottom outperform top-positioned menus on modern phones.
- Breadcrumbs are a symptom, not a cure. If you need breadcrumbs, your hierarchy might be too deep.

**Common failures:**
- Navigation that changes or disappears between screens
- No clear "home" or "back" affordance
- Deep nesting that requires breadcrumbs to navigate
- Critical actions buried in hamburger menus

### Forms & Inputs — Where Users Do the Work

Forms are where friction lives. Every unnecessary field, confusing label, or unhelpful error message costs you users.

**Rules:**
- Ask for the minimum information needed. Every additional field reduces completion rates. Think of it like a customs form — the shorter, the better.
- Labels go above inputs, not inside them (placeholder-as-label disappears on focus and breaks accessibility).
- Validate inline and in real time where possible. Don't make users submit a form to discover errors.
- Error messages must be specific and helpful: "Email must include @" not "Invalid input."
- Group related fields visually. Shipping address fields cluster together, separate from payment fields.
- Smart defaults save effort. Pre-select the most common option. Pre-fill what you already know.

**Common failures:**
- Placeholder text used as the only label
- Generic error messages ("Something went wrong")
- No inline validation — errors only shown after submission
- Asking for information you don't strictly need
- No autofocus on the first field

### States — Design for Reality, Not Just the Happy Path

An app that only looks good when everything works perfectly is only half-designed. Think of it like building a car — you design for rain, not just sunshine.

**Empty states:**
- What does every screen look like with zero data? If it looks broken, redesign it.
- Empty states are onboarding opportunities. Guide the user toward their first action.
- "No results found" should help, not dead-end. Suggest alternatives or adjustments.

**Loading states:**
- The app must feel alive while waiting. Skeleton screens > spinners > nothing.
- If a load takes more than 300ms, show a loading indicator. If more than 1 second, show progress.
- Optimistic UI (show the result before confirmation) makes things feel instant when appropriate.

**Error states:**
- Error messages must be human, not technical. "We couldn't save your changes. Try again?" not "Error 500: Internal Server Error."
- Always offer a way forward — retry, go back, contact support. Never a dead end.
- Style errors consistently across the entire app.

**Disabled states:**
- Disabled elements should look obviously disabled (reduced opacity, muted colour) and ideally explain *why* they're disabled via tooltip or helper text.
- If a button will become enabled after some action, tell the user what that action is.

**Hover, focus, and active states:**
- Every interactive element needs distinct hover, focus, and active states.
- Focus states are non-negotiable for keyboard accessibility. They must be visible.
- Touch devices don't have hover — don't hide critical information behind hover-only interactions.

### Motion & Animation — Physics, Not Decoration

Motion should feel like the natural consequence of an action, like how a ball bounces when dropped — governed by physics, not whimsy.

**Rules:**
- Every animation must have a purpose: guide attention, provide feedback, show spatial relationships, or smooth transitions.
- Duration: micro-interactions (button feedback) = 100–200ms. Transitions (page changes) = 200–400ms. Anything over 500ms feels sluggish.
- Use easing curves that mimic real physics: ease-out for entering elements, ease-in for exiting, ease-in-out for moving between positions.
- Respect `prefers-reduced-motion`. Some users experience motion sickness or have vestibular disorders. Provide reduced or no-motion alternatives.
- If an animation exists purely for "wow factor," remove it. Users stop noticing it after the second visit and it just slows them down.

**Common failures:**
- Animations that delay the user from completing tasks
- Inconsistent animation durations across the app
- No respect for reduced-motion preferences
- Bouncy/springy animations that feel playful in inappropriate contexts

---

## Part 4: Responsive Design

### Mobile-First Is Not a Suggestion

Design for the smallest screen first. Tablet and desktop are enhancements, not the other way around. Think of it like writing a tweet vs. an essay — if you can say it in 280 characters, you understand it well enough to expand.

**Rules:**
- Start layouts at 320px width. If it works there, scaling up is easy. Scaling down from desktop is almost always painful.
- Touch targets: minimum 44×44px. Thumbs are imprecise.
- Design for thumbs first, then cursors. Primary actions should be reachable in the thumb zone (bottom half of mobile screens).
- The layout should adapt fluidly across all viewport sizes — not just snap at 3 breakpoints. No screen size should feel like an afterthought.
- Test at actual device sizes, not just responsive browser windows. Browsers lie about touch behaviour and viewport rendering.

**Common failures:**
- Desktop-first design that's "made responsive" as a final step
- Touch targets too small for comfortable thumb use
- Horizontal scrolling on mobile
- Fixed-width elements that break on small screens
- Hover-dependent interactions with no touch fallback

### Breakpoint Strategy

Don't design for devices. Design for content. Let the content tell you when it needs a breakpoint — when a line of text gets too long, when a grid gets too cramped, when whitespace collapses.

That said, practical starting points:
- **Small (mobile):** 320–480px
- **Medium (tablet):** 481–768px
- **Large (desktop):** 769–1200px
- **Extra large:** 1200px+

Content width should max out around 1200–1440px. Beyond that, let whitespace grow on the sides — don't stretch content to fill ultrawide monitors.

---

## Part 5: Accessibility

### This Is Not Optional

Accessibility isn't a feature you bolt on. It's a design constraint you build within — like structural integrity in a building. You don't add it after the building is finished.

**Rules:**
- Semantic HTML first: use `<button>` for buttons, `<nav>` for navigation, `<h1>`–`<h6>` in order. Don't `<div>` everything and bolt on ARIA roles.
- Colour contrast: WCAG AA minimum (4.5:1 body text, 3:1 large text/components). AAA (7:1) where achievable.
- All images need meaningful `alt` text or `alt=""` for purely decorative images.
- Keyboard navigation must work for every feature. Tab order should follow visual order.
- Focus indicators must be visible. Never `outline: none` without a visible replacement.
- ARIA labels on interactive elements that lack visible text (icon-only buttons, etc.).
- Forms need associated `<label>` elements, error descriptions linked via `aria-describedby`.
- Screen reader flow should match the visual reading order.
- Test with actual assistive technology, not just automated checkers.

---

## Part 6: Performance as Design

### Speed Is a Feature

A beautiful interface that loads in 5 seconds is worse than a plain one that loads instantly. Performance IS the user experience for the first few seconds — and first impressions are permanent. It's like a restaurant — if you wait 20 minutes for a menu, the food better be incredible. But you've already formed your opinion.

**Rules:**
- Target first meaningful paint under 1.5 seconds on 3G.
- Lazy-load images and off-screen content. Don't make users download what they haven't scrolled to.
- Optimise images: use WebP/AVIF where supported, appropriate dimensions (don't serve a 4000px image in a 400px container), and consider progressive loading.
- Minimise layout shift (CLS). Nothing erodes trust like a page that jumps around while loading.
- Code-split routes. Users shouldn't download JavaScript for pages they haven't visited.
- Perceived performance matters as much as actual performance. Skeleton screens, progressive loading, and instant feedback make things *feel* fast even when the network is slow.

---

## Part 7: Content & Microcopy

### Words Are Interface

The text in your interface IS the interface for most interactions. A confusing label does more damage than a misaligned icon.

**Rules:**
- Use the user's language, not your codebase's language. "Your items" not "Cart entities." "Sign in" not "Authenticate."
- Button labels should describe outcomes: "Save changes" not "Submit." "Create account" not "Go."
- Be concise but not cryptic. "Delete this project? This can't be undone." is better than both "Delete?" and "Are you sure you want to permanently delete this project and all associated data? This action is irreversible and cannot be recovered."
- Error messages: say what went wrong AND what to do about it.
- Avoid jargon, abbreviations, and internal terminology.
- Maintain consistent terminology: if you call it "Workspace" on one screen, don't call it "Project" on another.

---

## Part 8: Design System Discipline

### The System Is the Source of Truth

A design system isn't a Figma file that gets ignored. It's the engineering specification that ensures consistency. Think of it like building codes — individual houses vary, but they all meet the same structural standards.

**Rules:**
- All values must reference design tokens — no hardcoded colours, spacing, font sizes, or border radii anywhere in the codebase.
- If a value doesn't exist in the system and you need it, propose it formally: "I need a new spacing token for X because Y." Don't invent rogue values.
- The same component must look and behave identically everywhere it appears. If you find inconsistency, fix the component — don't create a third variation.
- Document every component with its states: default, hover, focus, active, disabled, loading, error.
- Design tokens should be named semantically (`color-text-primary`, `spacing-section-gap`) not literally (`blue-500`, `margin-16`).

**Minimum design system files for any project:**

| File | Purpose |
|------|---------|
| `DESIGN_SYSTEM.md` | Tokens: colours, typography, spacing, shadows, radii, breakpoints |
| `FRONTEND_GUIDELINES.md` | Component architecture, file structure, state management |
| `APP_FLOW.md` | Every screen, route, and user journey |

---

## Part 9: Design Review Checklist

Use this for any PR that touches UI. Not every item applies to every change, but scan the full list.

### Visual Quality
- [ ] Visual hierarchy is clear — primary action is unmissable
- [ ] Spacing uses the defined scale consistently
- [ ] Typography follows the type scale (no rogue sizes or weights)
- [ ] Colours reference design tokens (no hardcoded values)
- [ ] Alignment is precise — nothing is off by 1–2px
- [ ] Icons are from the same set, consistent size and weight
- [ ] The screen passes the 2-second test

### Interaction Quality
- [ ] All states are designed: empty, loading, error, disabled, hover, focus, active
- [ ] Animations have purpose, appropriate duration, and respect `prefers-reduced-motion`
- [ ] Forms validate inline with specific, helpful error messages
- [ ] Touch targets are minimum 44×44px on touch devices
- [ ] No information or functionality is hidden behind hover-only interactions

### Responsiveness
- [ ] Layout works at 320px, 768px, 1024px, and 1440px+
- [ ] No horizontal scroll on any viewport
- [ ] Content reflows fluidly — not just at breakpoints
- [ ] Mobile navigation is thumb-reachable

### Accessibility
- [ ] Colour contrast meets WCAG AA (4.5:1 body, 3:1 large text)
- [ ] All interactive elements are keyboard-accessible
- [ ] Focus indicators are visible
- [ ] Images have appropriate alt text
- [ ] Screen reader flow matches visual order
- [ ] ARIA labels on icon-only buttons and non-text interactives

### Performance
- [ ] Images are optimised and appropriately sized
- [ ] Off-screen content is lazy-loaded
- [ ] No unnecessary layout shift during load
- [ ] Perceived performance is addressed (skeletons, progressive loading)

### Consistency
- [ ] Components match their existing instances elsewhere in the app
- [ ] Terminology is consistent with the rest of the app
- [ ] No rogue design values (colours, spacing, fonts) outside the system

---

## Part 10: Working with AI Design Agents

When using this document with AI coding tools (Claude Code, Cursor, Codex, Gemini CLI, etc.), follow this protocol:

### Before Any Design Work

The agent must read and internalise:
1. This file (`UI_Design_Philosophy.md`)
2. `DESIGN_SYSTEM.md` — existing visual tokens
3. `FRONTEND_GUIDELINES.md` — component engineering standards
4. `APP_FLOW.md` — all screens and user journeys
5. `PRD.md` — feature requirements
6. `TECH_STACK.md` — platform capabilities and constraints
7. `LESSONS.md` — mistakes and corrections from previous sessions

The agent must experience the live app at mobile, tablet, and desktop viewports before proposing changes. You must understand the current system completely before changing it.

### The Jobs Filter

For every element on every screen, the agent asks:
- "Would a user need to be told this exists?" → If yes, redesign until obvious.
- "Can this be removed without losing meaning?" → If yes, remove it.
- "Does this feel inevitable, like no other design was possible?" → If no, it's not done.

### Audit → Plan → Approve → Implement

1. **Audit** every screen against the principles in this document.
2. **Plan** findings into phases:
   - **Phase 1 — Critical:** Hierarchy, usability, responsiveness, or consistency issues that actively harm the experience.
   - **Phase 2 — Refinement:** Spacing, typography, colour, alignment adjustments that elevate the experience.
   - **Phase 3 — Polish:** Micro-interactions, transitions, empty/loading/error states, dark mode, and subtle details that make it feel premium.
3. **Present** the plan with specific, unambiguous implementation instructions. "Make the cards feel softer" is not an instruction. "CardComponent border-radius: 8px → 12px, shadow: 0 1px 3px rgba(0,0,0,0.12)" is.
4. **Wait for approval** before implementing anything.
5. **Execute** only what was approved.
6. **Review** after each phase before moving to the next.

### Scope Discipline for Agents

**Touch:** Visual design, layout, spacing, typography, colour, interaction design, motion, accessibility, design token proposals.

**Don't touch:** Application logic, state management, API calls, data models, feature additions/removals, backend anything.

If a design improvement requires a functionality change, flag it:
> "This design improvement would require [functional change]. That's outside design scope. Flagging for the build agent."

### After Implementation

- Update `LESSONS.md` with patterns and corrections discovered.
- Update `DESIGN_SYSTEM.md` if new tokens were introduced.
- Update agent instruction files so build agents pick up changes.
- Flag remaining approved-but-unimplemented phases.

---

## Appendix: The Remove-Until-It-Breaks Test

When evaluating any screen, systematically remove elements one at a time. For each removal, ask: "Did the user lose the ability to understand or complete their task?"

- If **no** → the element was unnecessary. Leave it out.
- If **yes** → add it back. That element earns its place.

Keep going until every remaining element is load-bearing. What you're left with is the design.

---

*This document should evolve. When you learn something about your users, your platform, or your mistakes — add it to `LESSONS.md` and update the relevant section here. A design philosophy that doesn't grow with the project is just decoration.*
