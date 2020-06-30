package com.google.sps;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.User;
import java.net.MalformedURLException;
import java.net.URLConnection;

/* A mock class that imitates the UserService API for testing */
public class MockedUserService implements UserService {

  private User currentUser;
  private boolean loggedIn;
  private static final String EMAIL_TAG = "email=";

  public MockedUserService() {
    currentUser = null;
    loggedIn = false;
  }

  @Override
  public String createLoginURL(String destinationURL) {
      return destinationURL + "login_url";
  }

  @Override
  public String createLoginURL(String destinationURL, String authDomain) {
    return createLoginURL(destinationURL);
  }

  @Override
  public String createLoginURL(String destinationURL,String authDomain,String federatedIdentity,java.util.Set<java.lang.String> attributesRequest) {
    return createLoginURL(destinationURL);
  }

  @Override
  public String createLogoutURL(String destinationURL) {
    return(destinationURL + "logout_url");
  }

  @Override
  public String createLogoutURL(String destinationURL, String authDomain) {
    return createLogoutURL(destinationURL);
  }

  @Override
  public User getCurrentUser() {
    if(!loggedIn) {
      return null;
    }
    return currentUser;
  }

  @Override
  public boolean isUserAdmin() {
    return false;
  }

  @Override
  public boolean isUserLoggedIn() {
    return loggedIn;
  }

  public URLConnection evaluateURL(String url) throws MalformedURLException {
    if(url.contains("login")) {
      login(url.substring(url.indexOf(EMAIL_TAG) + EMAIL_TAG.length()));
    } else if (url.contains("logout")) {
      logout();
    } else {
      throw new MalformedURLException("invalid url");
    }
    return null;
  }

  private void login(String email) {
    currentUser = new User(email, "");
    loggedIn = true;
  }

  private void logout() {
    loggedIn = false;
  }
}
