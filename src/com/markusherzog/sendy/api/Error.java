package com.markusherzog.sendy.api;

/**
 * <err code="3001" msg="Invalid Posterous email or password" />
 * 3001 - Invalid Posterous email or password
 * 3002 - Invalid site id
 * 3003 - User does not have access to this site
 * 3004 - Unable to save post
 * 3005 - Unable to find objects for site
 * 3006 - Invalid date
 * 3007 - Invalid post id. Post not found
 * 3008 - Comments disabled for site
 * 3009 - Unable to save comment
 * 3010 - API rate limit reached
 * @author markus
 *
 */
public class Error {
	
	public static final int Invalid_Posterous_email_or_password = 3001; // Invalid Posterous email or password
	public static final int Invalid_site_id = 3002; // Invalid site id
	public static final int User_does_not_have_access_to_this_site = 3003; // User does not have access to this site
	public static final int Unable_to_save_post = 3004; // Unable to save post
	public static final int Unable_to_find_objects_for_site = 3005; // Unable to find objects for site
	public static final int Invalid_date = 3006; // Invalid date
	public static final int Invalid_post_id_Post_not_found = 3007; // Invalid post id. Post not found
	public static final int Comments_disabled_for_site = 3008; // Comments disabled for site
	public static final int Unable_to_save_comment = 3009; // Unable to save comment
	public static final int API_rate_limit_reached = 3010; // API rate limit reached
	
	int code;
	String msg;
}
