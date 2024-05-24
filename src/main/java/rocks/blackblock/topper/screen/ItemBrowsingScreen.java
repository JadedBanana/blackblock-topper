package rocks.blackblock.topper.screen;

import net.minecraft.server.network.ServerPlayerEntity;
import rocks.blackblock.screenbuilder.ScreenBuilder;
import rocks.blackblock.screenbuilder.inputs.EmptyInput;
import rocks.blackblock.screenbuilder.interfaces.WidgetDataProvider;
import rocks.blackblock.screenbuilder.widgets.PaginationWidget;

public abstract class ItemBrowsingScreen extends EmptyInput implements WidgetDataProvider {

    protected ServerPlayerEntity player;
    protected int page = 1;

    /**
     * Set up pagination.
     *
     * @author  Jade Godwin         <icanhasabanana@gmail.com>
     * @since    0.1.1
     */
    protected void setUpPagination(ScreenBuilder sb, int max_page_count) {
        // Set up pagination.
        PaginationWidget pagination = new PaginationWidget();
        pagination.setId("pagination");
        pagination.setSlotIndex(50);
        pagination.setMaxValue(max_page_count);

        // On a page change, replace the whole screen.
        pagination.setOnChangeListener((texturedScreenHandler, widget) -> {
            texturedScreenHandler.replaceScreen(this);
        });

        // Add paginator and return.
        sb.addWidget(pagination);
    }

    @Override
    public Object getWidgetValue(String widget_id) {
        if (widget_id.equals("pagination")) { return this.page; }
        return null;
    }

    @Override
    public void setWidgetValue(String widget_id, Object value) {
        if (widget_id.equals("pagination")) { this.page = (int) value; }
    }

}
