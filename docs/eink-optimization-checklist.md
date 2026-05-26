# E-Ink Optimization Checklist for Boox 3 Air C

Before release, verify each point on the physical device:

## Display
- [ ] Window default is UpdateMode.GU
- [ ] RecyclerViews use UpdateMode.ANIMATION while scrolling
- [ ] Clean (GC) refresh triggers when scrolling stops
- [ ] No ghosting after 30s of static display
- [ ] Reflection drawing uses UpdateMode.DU for low latency
- [ ] Full GC refresh triggers on Activity resume

## Stylus
- [ ] Checkmark gesture completes habit on first try
- [ ] Strike-through gesture strikes through todo
- [ ] Pressure sensitivity works (thin/thick strokes)
- [ ] Eraser (back of stylus) works in reflection area
- [ ] No lag (>100ms) between pen touch and ink appearing
- [ ] Pen hovers don't cause unnecessary screen refreshes

## Widget
- [ ] Widget renders correctly on Boox home screen
- [ ] Widget updates when data changes
- [ ] Widget shows current date, habits, and todos

## Performance
- [ ] App launches in under 2 seconds
- [ ] Scrolling doesn't stutter
- [ ] Battery drain less than 2% per hour of use
- [ ] No screen flashing during normal use (only intentional clean refreshes)

## Atomic Habits
- [ ] Identity statement shown on daily dashboard header
- [ ] Streak counter visible for each habit
- [ ] "Never miss twice" prompt shown when yesterday was missed
- [ ] Habit stacking order visible in habit detail