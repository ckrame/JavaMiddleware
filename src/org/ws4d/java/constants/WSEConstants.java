package org.ws4d.java.constants;

public interface WSEConstants {

	/** The default prefix for the WSE namespace. */
	public static final String	WSE_NAMESPACE_PREFIX			= "wse";

	/** "EventSource". */
	public static final String	WSE_ATTR_EVENTSOURCE			= "EventSource";

	/** "Mode" within "Delivery". */
	public static final String	WSE_ATTR_DELIVERY_MODE			= "Mode";

	public static final String	WSE_ATTR_FILTER_DIALECT			= "Dialect";

	/* The elements tag names for WS Eventing */
	public static final String	WSE_ELEM_CODE					= "Code";

	public static final String	WSE_ELEM_DELIVERY				= "Delivery";

	/** "EndTo". */
	public static final String	WSE_ELEM_ENDTO					= "EndTo";

	public static final String	WSE_ELEM_EXPIRES				= "Expires";

	public static final String	WSE_ELEM_FILTER					= "Filter";

	public static final String	WSE_ELEM_GETSTATUS				= "GetStatus";

	public static final String	WSE_ELEM_GETSTATUSRESPONSE		= "GetStatusResponse";

	public static final String	WSE_ELEM_IDENTIFIER				= "Identifier";

	/** "NotifyTo". */
	public static final String	WSE_ELEM_NOTIFYTO				= "NotifyTo";

	public static final String	WSE_ELEM_REASON					= "Reason";

	public static final String	WSE_ELEM_RENEW					= "Renew";

	public static final String	WSE_ELEM_RENEWRESPONSE			= "RenewResponse";

	public static final String	WSE_ELEM_STATUS					= "Status";

	public static final String	WSE_ELEM_SUBSCRIBE				= "Subscribe";

	public static final String	WSE_ELEM_UNSUBSCRIBERESPONSE	= "UnsubscribeResponse";

	public static final String	WSE_ELEM_SUBSCRIBERESPONSE		= "SubscribeResponse";

	public static final String	WSE_ELEM_SUBSCRIPTIONEND		= "SubscriptionEnd";

	/** "SubscriptionManager". */
	public static final String	WSE_ELEM_SUBSCRIPTIONMANAGER	= "SubscriptionManager";

	public static final String	WSE_ELEM_UNSUBSCRIBE			= "Unsubscribe";

	public static final String	WSE_FILTER_EVENTING_ACTION		= "/Action";

}
