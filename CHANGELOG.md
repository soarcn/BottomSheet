ChangeLog
============

1.3.1
-------
- Fix crash on Android O

1.3.0
-------
- Defects fixing
- API break
    - change style attribute "bottomSheetStyle" to "bs_bottomSheetStyle" #108
    

1.2.0
--------

- Defects fixing
- Developers can fully customize the whole ui

1.1.1
-------
- Defect fixing #35

1.1.0
-------

- Compatible with android menu xml, replace divider with group.
- Menu items now could be accessed and modified through android menu api. Get a menu object with getMenu();
- API break
    - create() been removed from builder, use build() instead.
    - divider() been removed from builder, use <group> tag in xml.
    - most of sheet() methods been deprecated