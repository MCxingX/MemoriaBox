# Calendar Theme UX Upgrade Task List

## P0: Core Calendar and Import Trust

### 1. Add theme semantic tokens

- [x] 1.1 Create `MemoriaThemeTokens` and `LocalMemoriaThemeTokens`.
- [x] 1.2 Map tokens for all existing `AppThemeMode` values.
- [x] 1.3 Provide safe Material colorScheme fallback values.
- [x] 1.4 Replace calendar-specific hardcoded colors with token reads.
- [ ] 1.5 Verify dark mode and soft themes keep readable contrast.

### 2. Refine calendar date states

- [ ] 2.1 Define `CalendarDayUiState` for date grid rendering.
- [ ] 2.2 Implement visual priority for selected date, today, content and outside-month dates.
- [x] 2.3 Keep diary small marker behavior compatible with existing monthly diary summary feature.
- [x] 2.4 Add multi-marker rendering for diary, anniversary, festival and todo placeholders.
- [ ] 2.5 Add accessibility descriptions for date cells.

### 3. Add selected day summary card

- [ ] 3.1 Define `SelectedDaySummaryUiState`.
- [x] 3.2 Render selected date, lunar text and content counts.
- [x] 3.3 Show anniversary countdown or elapsed days when available.
- [x] 3.4 Show diary count and nearest diary preview when available.
- [x] 3.5 Show empty state and creation entry when selected date has no content.
- [ ] 3.6 Validate compact layout and font scaling.

### 4. Add backup import confirmation and result summary

- [ ] 4.1 Add `ImportResult` model in backup flow.
- [ ] 4.2 Update `BackupManager.importBackup` to return structured counts.
- [x] 4.3 Add import confirmation dialog with merge-import explanation.
- [x] 4.4 Keep password input masked for encrypted backups.
- [ ] 4.5 Add import result dialog with added, updated, skipped and failed counts.
- [ ] 4.6 Test imports preserve existing data.

### 5. Protect confirmed core interaction

- [x] 5.1 Verify bottom navigation middle item still uses random kaomoji.
- [x] 5.2 Allow add guidance only during onboarding.
- [x] 5.3 Ensure finished onboarding leaves the middle item as pure random kaomoji with no badge, tooltip or reminder.
- [x] 5.4 Add a regression check note or UI test target for the random kaomoji behavior.
- [x] 5.5 Avoid replacing the middle item with a generic add icon during UI cleanup.

### 5b. Fix friend birthday sorting

- [x] 5b.1 Locate friend management list and delete flow.
- [x] 5b.2 Sort birthdays within one month by nearest date first.
- [x] 5b.3 Keep birthdays beyond one month in the list after near birthdays.
- [x] 5b.4 Keep friends without birthdays in the list after dated friends.
- [x] 5b.5 Verify deleted friends disappear and remaining friends keep the sorting rule.

## P1: Theme and Typography Maturity

### 6. Improve Chinese typography hierarchy

- [x] 6.1 Expand `Type.kt` with title, body, label and supporting text levels.
- [x] 6.2 Apply typography levels to calendar header, date cells, summary card and setting sections.
- [ ] 6.3 Validate long Chinese labels, mixed numeric text and system font scaling.
- [ ] 6.4 Keep line heights comfortable in compact and roomy layouts.

### 7. Add theme groups in settings

- [x] 7.1 Group themes into recommended, eye-care, playful and dark categories.
- [x] 7.2 Add short theme descriptions.
- [x] 7.3 Highlight current theme with clear selected state.
- [x] 7.4 Make recommended themes align with warm, cream, mint and lavender directions.

### 8. Add theme preview wall

- [x] 8.1 Build `ThemePreviewCard` component.
- [x] 8.2 Show mini calendar date, selected state, markers and summary color blocks.
- [x] 8.3 Use adaptive grid or horizontal scrolling for narrow screens.
- [x] 8.4 Apply theme on preview click using existing settings persistence.

### 9. Add marker legend

- [x] 9.1 Add a compact legend for diary, anniversary, festival and todo markers.
- [x] 9.2 Use labels plus colors so meaning does not depend on color alone.
- [ ] 9.3 Place legend near calendar or inside help/overflow area based on available space.

## P2: Calendar View Expansion

### 10. Add week view

- [x] 10.1 Define `CalendarDisplayMode.Month` and `CalendarDisplayMode.Week`.
- [x] 10.2 Add UI toggle for month and week view.
- [x] 10.3 Render current week with seven day cells and content markers.
- [x] 10.4 Support previous and next week navigation.
- [x] 10.5 Sync selected day summary when a week day is selected.

### 11. Add agenda list view

- [x] 11.1 Extend display mode with `Agenda`.
- [x] 11.2 Build date-grouped list for nearby diaries and anniversaries.
- [x] 11.3 Add empty state and creation entry.
- [ ] 11.4 Use lazy list for larger datasets.
- [ ] 11.5 Ensure agenda items use theme tokens and typography levels.

### 12. Add date jump

- [x] 12.1 Add month title click or explicit jump action.
- [x] 12.2 Implement year-month picker.
- [x] 12.3 Jump to selected month and keep state consistent.
- [x] 12.4 Add return-to-today action.

### 13. Add monthly memory heat map

- [x] 13.1 Compute daily heat level from diary, anniversary and todo counts.
- [x] 13.2 Render heat intensity using theme heat tokens.
- [x] 13.3 Add empty state for months without records.
- [ ] 13.4 Ensure heat map remains readable in dark mode.

## P3: Emotional and Productivity Extensions

### 14. Add anniversary story cards

- [ ] 14.1 Define story card UI model from anniversary and linked diary data.
- [x] 14.2 Render upcoming anniversary story cards.
- [x] 14.3 Use linked image or themed placeholder visual.
- [ ] 14.4 Navigate to anniversary or diary detail on click.

### 15. Add birthday relationship reminders

- [ ] 15.1 Add relationship label support for birthday-type anniversaries.
- [ ] 15.2 Prioritize upcoming birthdays in summary and agenda.
- [ ] 15.3 Display age or countdown information.
- [ ] 15.4 Fall back to generic birthday display when relationship is empty.

### 16. Add lightweight todo board

- [ ] 16.1 Define todo data model and persistence approach.
- [ ] 16.2 Add todo creation for date or anniversary.
- [x] 16.3 Show todo marker in calendar date cells.
- [x] 16.4 Show todo preview in selected day summary and agenda.
- [ ] 16.5 Support quick complete action.

### 17. Sync widget theme

- [ ] 17.1 Map app theme tokens to widget-safe colors.
- [x] 17.2 Refresh widgets after theme changes.
- [ ] 17.3 Validate widget readability in dark and light system modes.
- [ ] 17.4 Use simplified token fallback where widget constraints apply.

## Verification

- [x] Run debug Kotlin compilation.

```bash
# Compile debug Kotlin sources
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk && /tmp/gradle-8.7/bin/gradle :app:compileDebugKotlin --no-daemon
```

- [x] Build debug APK.

```bash
# Assemble debug APK
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk && /tmp/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon
```

- [ ] Manually verify all app themes on the calendar screen.
- [ ] Manually verify compact and roomy screen behavior.
- [ ] Manually verify system font scaling.
- [ ] Manually verify encrypted and unencrypted backup import flows.
- [x] Manually verify bottom navigation middle item still displays random kaomoji.

## Implementation Notes

- `:app:compileDebugKotlin` passed on 2026-06-08.
- `:app:assembleDebug` passed on 2026-06-08.
- Backup import currently shows a merge-preserving confirmation and a concise result summary. Structured added, updated, skipped and failed counts remain a follow-up.
- Friend management was added as a complete lightweight flow using existing `friends` persistence.
- Widget theme synchronization currently refreshes existing widgets after theme changes. Widget-safe color remapping remains a follow-up.

## Recommended Implementation Order

1. P0 task 1: theme semantic tokens.
2. P0 task 2: calendar date states.
3. P0 task 3: selected day summary card.
4. P0 task 4: backup import confirmation and result summary.
5. P1 tasks 6 to 8: typography and theme settings maturity.
6. P2 tasks 10 to 13: week, agenda, date jump and heat map.
7. P3 tasks 14 to 17: story cards, birthday relationship, todo board and widget theme sync.
