ChangeLog
============

1.1.0
-------

- Compatible with android menu xml, replace divider with group.
- Menu items now could be accessed and modified through android menu api. Get a menu object with getMenu();
- API break
    - create() been removed from builder, use build() instead.
    - divider() been removed from builder, use <group> tag in xml.
    - most of sheet() methods been deprecated