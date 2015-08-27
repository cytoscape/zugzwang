# zugzwang
Zugzwang (German: forced to make a move) is a new OpenGL-accelerated renderer for Cytoscape that prioritizes rapid visual feedback over everything else – thus the name.


Usage:
1. Compile with 'mvn clean install' and put into Cytoscape's app folder, like any other app.
2. When creating a new network view, Cytoscape should offer to choose a rendering engine. Choose Zugzwang instead of Cytoscape2D here.
3. No context menu for node creation yet, so open an existing network.
4. Fit network isn't implemented yet, so you'll probably have to zoom out first to see anything.
5. Apply a layout to pull the nodes apart, zoom out to see everything.
6. Controls: Wheel scroll = zoom; wheel press and drag (or +Ctrl) = pan view (or rotate).