package com.googlecode.charpa.web.progress;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.time.Duration;

import com.googlecode.charpa.progress.service.IProgressInfo;
import com.googlecode.charpa.progress.service.IProgressInfoService;
import com.googlecode.charpa.progress.service.LogMessage;
import com.googlecode.charpa.progress.service.ProgressId;
import com.googlecode.charpa.progress.service.ProgressState;
import com.googlecode.charpa.progress.service.impl.DefaultResourceResolver;
import com.googlecode.charpa.progress.service.spi.IResourceResolver;
import com.googlecode.charpa.web.component.ConfirmAjaxLink;
import com.googlecode.charpa.web.util.FormatUtils;

/**
 * Displays progress info
 */
public class ProgressPanel extends Panel {
	
	public ProgressPanel(String aId, final PageParameters aParameters, IProgressInfoService aProgressInfoService) {
		this(aId, aParameters, aProgressInfoService, new DefaultResourceResolver());
	}

    public ProgressPanel(String aId, final PageParameters aParameters, IProgressInfoService aProgressInfoService,
    		IResourceResolver aResourceResolver) {
        super(aId);

        theProgressInfoService = aProgressInfoService;
        
        String idString = aParameters.getString("id");

        if(idString==null || idString.trim().length()==0) throw new IllegalStateException("There was no id parameter given");

        WebMarkupContainer panel = new WebMarkupContainer("panel");
        add(panel);

        final ProgressId id = new ProgressId(idString);

        final AbstractReadOnlyModel<IProgressInfo> model = new AbstractReadOnlyModel<IProgressInfo>() {
            public IProgressInfo getObject() {
                return theProgressInfoService.getProgressInfo(id);
            }
        };

        setDefaultModel(model);
        panel.add(new Label("progress-name", new CompoundPropertyModel<String>(model).bind("name")));
        panel.add(new Label("progress-text", new CompoundPropertyModel<String>(model).bind("progressText")));
        panel.add(new Label("progress-max", new CompoundPropertyModel<String>(model).bind("max")));
        panel.add(new Label("progress-value", new CompoundPropertyModel<String>(model).bind("currentValue")));
        panel.add(new Label("progress-state", new ResolvingModel(aResourceResolver, "state." + model.getObject().getState().name(), model.getObject().getState().name())));

        WebMarkupContainer progressDone = new WebMarkupContainer("progress-done");
        panel.add(progressDone);
        progressDone.add(new AttributeModifier("width", true, new AbstractReadOnlyModel<Object>() {
            public Object getObject() {
                IProgressInfo info = model.getObject();
                return Math.round((info.getCurrentValue() / (float)info.getMax()) * 400);
            }
        }));

        WebMarkupContainer progressToBeDone = new WebMarkupContainer("progress-tobedone");
        panel.add(progressToBeDone);
        progressToBeDone.add(new AttributeModifier("width", true, new AbstractReadOnlyModel<Object>() {
            public Object getObject() {
                IProgressInfo info = model.getObject();
                return 400 - Math.round((info.getCurrentValue() / (float)info.getMax()) * 400);            
            }
        }));

        panel.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(3)) {
            @SuppressWarnings("unchecked")
			@Override
            protected void onPostProcessTarget(AjaxRequestTarget aTarget) {
                IProgressInfo info = (IProgressInfo) model.getObject();
                PageParameters pageParameters = info.getPageParameters().isEmpty()
                                    ? aParameters : new PageParameters(info.getPageParameters());
                if(pageParameters.get(ProgressParameters.NEXT_PAGE)!=null) {
                    try {
                        Class<? extends Page> pageClass = (Class<? extends Page>) Class.forName(pageParameters.getString(ProgressParameters.NEXT_PAGE));
                        if(info.getState() == ProgressState.FINISHED) {
                            setResponsePage(pageClass, pageParameters);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
        });

        ConfirmAjaxLink cancelLink = new ConfirmAjaxLink("cancel-link", new ResolvingModel(aResourceResolver, CANCEL_CONFIRMATION_KEY, "Are you sure to cancel project run?")) {
            public void onClick(AjaxRequestTarget target) {
                theProgressInfoService.cancelProgress(id);
            }

            public boolean isVisible() {
                IProgressInfo info = model.getObject();
                return info.getState() == ProgressState.PENDING || info.getState() == ProgressState.RUNNING;
            }
        };
		panel.add(cancelLink);
		cancelLink.add(new Label("cancel-label", new ResolvingModel(aResourceResolver, CANCEL_KEY, "Cancel")));

        // time
		panel.add(new Label("created-time-label", new ResolvingModel(aResourceResolver, CREATED_TIME_KEY, "Created time")));
        panel.add(new Label("created-time", new AbstractReadOnlyModel<String>() {
            public String getObject() {
                return FormatUtils.formatDateTime(model.getObject().getCreatedTime());
            }
        }) {
            public boolean isVisible() {
                return model.getObject().getCreatedTime()!=null;
            }
        });

        panel.add(new Label("started-time-label", new ResolvingModel(aResourceResolver, STARTED_TIME_KEY, "Started time")));
        panel.add(new Label("started-time", new AbstractReadOnlyModel<String>() {
            public String getObject() {
                return FormatUtils.formatDateTime(model.getObject().getStartedTime());
            }
        }) {
            public boolean isVisible() {
                return model.getObject().getStartedTime()!=null;
            }
        });

        panel.add(new Label("ended-time-label", new ResolvingModel(aResourceResolver, ENDED_TIME_KEY, "Ended time")));
        panel.add(new Label("ended-time", new AbstractReadOnlyModel<String>() {
            public String getObject() {
                return FormatUtils.formatDateTime(model.getObject().getEndedTime());
            }
        }) {
            public boolean isVisible() {
                return model.getObject().getEndedTime()!=null;
            }
        });

        panel.add(new Label("time-elapsed-label", new ResolvingModel(aResourceResolver, TIME_ELAPSED_KEY, "Time elapsed time")));
        panel.add(new Label("time-elapsed", new AbstractReadOnlyModel<String>() {
            public String getObject() {
                return FormatUtils.formatPeriod(model.getObject().getElapsedPeriod());
            }
        }) {
            public boolean isVisible() {
                return model.getObject().getElapsedPeriod()!=null;
            }
        });

        panel.add(new Label("estimated-time-left-label", new ResolvingModel(aResourceResolver, ESTIMATED_TIME_KEY, "Estimated time left")));
        panel.add(new Label("time-left", new AbstractReadOnlyModel<String>() {
            public String getObject() {
                return FormatUtils.formatPeriod(model.getObject().getLeftPeriod());
            }
        }) {
            public boolean isVisible() {
                return model.getObject().getLeftPeriod()!=null;
            }
        });

        // log messages
        LoadableDetachableModel<List<LogMessage>> logMessagesModel = new LoadableDetachableModel<List<LogMessage>>() {
            protected List<LogMessage> load() {
                return theProgressInfoService.getLastLogMessages(id, 20);
            }
        };
        panel.add(new ListView<LogMessage>("log-messages", logMessagesModel) {
            protected void populateItem(ListItem<LogMessage> aItem) {
                aItem.add(new Label("log-message", aItem.getModelObject().getMessage()));
            }
        });
    }
    
    public static final String CANCEL_KEY = "cancel";
    public static final String CANCEL_CONFIRMATION_KEY = "cancel.confirmation";
    public static final String CREATED_TIME_KEY = "created.time";
    public static final String STARTED_TIME_KEY = "started.time";
    public static final String ENDED_TIME_KEY = "ended.time";
    public static final String TIME_ELAPSED_KEY = "time.elapsed";
    public static final String ESTIMATED_TIME_KEY = "estimated.time.left";

    @SuppressWarnings({"UnusedDeclaration"})

    private final IProgressInfoService theProgressInfoService;
    
    private static class ResolvingModel extends AbstractReadOnlyModel<String> {
    	
    	private IResourceResolver resourceResolver;
    	private String key;
    	private String defaultValue;

		public ResolvingModel(IResourceResolver resourceResolver, String key,
				String defaultValue) {
			super();
			this.resourceResolver = resourceResolver;
			this.key = key;
			this.defaultValue = defaultValue;
		}

		@Override
		public String getObject() {
			return resourceResolver.resolve(key, defaultValue);
		}
    	
    }
}