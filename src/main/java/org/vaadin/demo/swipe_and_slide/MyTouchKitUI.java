package org.vaadin.demo.swipe_and_slide;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.vaadin.addon.touchkit.ui.NavigationBar;
import com.vaadin.addon.touchkit.ui.NavigationButton;
import com.vaadin.addon.touchkit.ui.NavigationManager;
import com.vaadin.addon.touchkit.ui.NavigationManager.NavigationEvent.Direction;
import com.vaadin.addon.touchkit.ui.SwipeView;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.UI;

/**
 * The UI's "main" class
 */
@Widgetset("org.vaadin.demo.swipe_and_slide.gwt.AppWidgetSet")
@Theme("touchkit")
public class MyTouchKitUI extends UI {
    @Override
    public void attach() {
        super.attach();
        VaadinSession session = getUI().getSession();
        session.addRequestHandler(new RequestHandler() {

            @Override
            public boolean handleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response)
                    throws IOException {
                String requestPathInfo = request.getPathInfo();
                if (requestPathInfo.contains("winterphotos/")) {
                    response.setCacheTime(60 * 60 * 1000);
                    response.setContentType("image/jpeg");

                    String ss = requestPathInfo.substring(requestPathInfo
                            .lastIndexOf("/") + 1);
                    InputStream resourceAsStream = getClass()
                            .getResourceAsStream("/winterphotos/" + ss);
                    IOUtils.copy(resourceAsStream, response.getOutputStream());
                    return true;
                }
                return false;
            }
        });
    }

    public static class SwipeViewTestMgr extends NavigationManager {

        int index = 0;

        SwipeView[] images;

        private boolean skipNextAutoViewChange = false;

        public SwipeViewTestMgr() {

            addNavigationListener(new NavigationListener() {
                @Override
                public void navigate(NavigationEvent event) {
                    skipNextAutoViewChange = true;
                }
            });
            

            new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(4000);
                            if (!isConnectorEnabled()) {
                                break;
                            }
                            if (!skipNextAutoViewChange) {
                                final VaadinSession session = getSession();
                                session.lock();
                                try {
                                    navigateTo(getNextComponent());
                                } finally {
                                    session.unlock();
                                }
                            }
                            skipNextAutoViewChange = false;
                        } catch (Exception e) {
                            break;
                        }
                    }
                };
            }.start();

            setMaintainBreadcrumb(false);
            setWidth("100%");
            images = loadImages();

            SwipeView prev = getImage(index - 1);
            SwipeView cur = getImage(index);
            SwipeView next = getImage(index + 1);
            setPreviousComponent(prev);
            setCurrentComponent(cur);
            setNextComponent(next);
            updateNextPreviousInCurrentCompoenent();

            addNavigationListener(new NavigationListener() {
                public void navigate(NavigationEvent event) {
                    if (event.getDirection() == Direction.FORWARD) {
                        index++;
                        int nextViewIndex = index + 1;
                        while (nextViewIndex >= images.length) {
                            nextViewIndex -= images.length;
                        }
                        SwipeView next = getImage(nextViewIndex);
                        setNextComponent(next);
                    } else {
                        index--;
                        int i = index - 1;
                        while (i < 0) {
                            i += images.length;
                        }
                        SwipeView prev = getImage(i);
                        setPreviousComponent(prev);
                    }
                    updateNextPreviousInCurrentCompoenent();
                }
            });

        }

        private void updateNextPreviousInCurrentCompoenent() {
            Component currentComponent2 = getCurrentComponent();

            if (currentComponent2 instanceof ImageView) {
                ImageView imageView = (ImageView) currentComponent2;
                NavigationButton leftComponent = (NavigationButton) imageView.navigationBar
                        .getLeftComponent();
                leftComponent.setTargetView(getPreviousComponent());
                NavigationButton rightComponent = (NavigationButton) imageView.navigationBar
                        .getRightComponent();
                rightComponent.setTargetView(getNextComponent());
            }
        }

        private SwipeView getImage(int i) {
            while (i < 0) {
                i += images.length;
            }
            return images[i % images.length];
        }

        private SwipeView[] loadImages() {
            String[] filenames = new String[] { "Peimari during winter.jpg",
                    "Peimari, another skier.jpg",
                    "Perfect sunshine on Peimari ice.jpg",
                    "Sanders_fished_from_Peimari_.jpg",
                    "Snow_trees_and_sunshine_ in_Trysil.jpg",
                    "Snowy view in Trysil.jpg", "Sunset in Trysil.jpg",
                    "Swamp in Trysil during the winter.jpg",
                    "Track and shadow in powder snow.jpg",
                    "Trysil, break before reaching the peak.jpg",
                    "View to south on Peimari ice.jpg" };

            SwipeView[] ss = new SwipeView[filenames.length];

            for (int i = 0; i < filenames.length; i++) {
                final String f = filenames[i];
                ss[i] = new ImageView(f);
                ss[i].setId("l" + i);
            }

            return ss;
        }

        static class ImageView extends SwipeView {

            private String ss;
            private Embedded embedded = new Embedded();
            private NavigationBar navigationBar;
            private CssLayout layout = new CssLayout() {
                @Override
                protected String getCss(Component c) {
                    if (c == navigationBar) {
                        // Make background of bar semitranparent over the image.
                        return "background: rgba(255, 255, 255, 0.7); position:absolute;top:0;left:0;right:0;";
                    }
                    return super.getCss(c);
                }
            };

            public ImageView(String f) {
                setWidth("100%");
                setContent(layout);
                ss = f;
                navigationBar = new NavigationBar();
                NavigationButton button = new NavigationButton("<");
                button.setStyleName("back");
                navigationBar.setLeftComponent(button);
                navigationBar.setCaption(ss.replace(".jpg", "").replace("_",
                        " "));
                button = new NavigationButton(">");
                navigationBar.setRightComponent(button);
                layout.addComponent(navigationBar);
                button.setStyleName("forward");
                embedded.setWidth("100%");
                layout.addComponent(embedded);
            }

            @Override
            public void attach() {
                super.attach();

                UI ui = getUI();
                if (ui == null) {
                    throw new RuntimeException("WTF!!");
                }
                ExternalResource source = new ExternalResource(Page
                        .getCurrent().getLocation().getPath()
                        + "winterphotos/" + ss);
                embedded.setSource(source);
            }

            @Override
            public void detach() {
                super.detach();
            }
        }

    }

    @Override
    protected void init(VaadinRequest request) {
        // Hack server push with progress indicator
        final ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setIndeterminate(true);
        CssLayout l = new CssLayout() {
            @Override
            protected String getCss(Component c) {
                if(c == progressIndicator) {
                    return "margin-left:-10000";
                }
                return super.getCss(c);
            }
        };
        l.setSizeFull();
        l.addComponent(new SwipeViewTestMgr());
        l.addComponent(progressIndicator);
        setContent(l);
    }
}