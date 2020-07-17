// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/* global firebase */
/* global firebaseui */

/** *********************************************************************
 * Utility methods/onload methods (all pages)
 ***********************************************************************/
let url = '';
let loggedIn = false;

// Firebase configuration
const firebaseConfig = {
  apiKey: 'AIzaSyBt4BitYGc3aUw4aGVpLGyrXnJZbAXX9RE',
  authDomain: 'unitebystep.firebaseapp.com',
  databaseURL: 'https://unitebystep.firebaseio.com',
  projectId: 'unitebystep',
  storageBucket: 'unitebystep.appspot.com',
  messagingSenderId: '654189328956',
  appId: '1:654189328956:web:22ec1bce47a9cc1972e139',
  measurementId: 'G-QCZ0TV4734',
};

/**
 * Initializes Firebase using the imported Firebase JS files
 */
function initializeFirebase() {
  firebase.initializeApp(firebaseConfig);
  firebase.analytics();
}

/**
 * determines which stylesheet to use and generates nav bar
 *
 * @param {function} doAfter used to specify actions during checkLogin()
 */
function loadActions(doAfter) {
  const styleSheetElement = document.getElementById('style');
  if (window.innerWidth >= window.innerHeight) {
    styleSheetElement.href = '/style.css';
  } else {
    styleSheetElement.href = 'style-mobile.css';
  }

  initializeFirebase();

  firebase.auth().onAuthStateChanged(function(user) {
    if (user) {
      loggedIn = true;
    } else {
      loggedIn = false;
    }
    if (doAfter != null) {
      doAfter();
    }
    generateNavBar();
  }, function(error) {
    console.log(error);
  });
}

/**
 * DEPRECATED: Use Firebase system instead
 * checks for login status and fetches login/logout url
 *
 * @param {function} doAfter Will call this function after handling login
 *      Helps with chaining async functions and cleaning up code
 */
async function checkLogin(doAfter = null) {
  fetch('/auth').then((response) => response.json())
      .then(function(responseJson) {
        loggedIn = responseJson.loggedIn;
        url = responseJson.url;

        generateNavBar();
        if (doAfter != null) {
          doAfter();
        }
      });
}

/**
 * Logs the user out of Firebase
 */
function firebaseLogout() {
  firebase.auth().signOut().then(function() {
    window.location.href = '/index.html';
  }).catch(function(error) {
    console.log(error);
  });
}

/**
 * Retrieves a token to identify the user when communicating with the backend
 */
async function getUserIDToken() {
  return new Promise((resolve) => {
    if (loggedIn) {
      firebase.auth().currentUser
          .getIdToken(/* forceRefresh */ true)
          .then(function(idToken) {
            resolve(idToken);
          }).catch(function(error) {
            console.log(error);
          });
    } else {
      resolve('');
    }
  });
}


/**
 * Generates navigation bar for the page
 */
function generateNavBar() {
  const headerLeft = document.createElement('div');
  headerLeft.className = 'header-left';

  const logoLink = document.createElement('a');
  logoLink.href = '/index.html';
  const logo = document.createElement('img');
  logo.src = 'images/uniteLogo.svg';
  logo.alt = 'Unite by STEP logo.';
  headerLeft.appendChild(logoLink);
  logoLink.appendChild(logo);

  const homeLink = document.createElement('a');
  homeLink.className = 'nav-item';
  homeLink.href = '/index.html';
  homeLink.innerText = 'Home';
  headerLeft.appendChild(homeLink);

  const createLink = document.createElement('a');
  createLink.className = 'nav-item';
  createLink.href = '/create-event.html';
  createLink.innerText = 'Create';
  headerLeft.appendChild(createLink);

  const findLink = document.createElement('a');
  findLink.className = 'nav-item';
  findLink.href = '/search.html';
  findLink.innerText = 'Find';
  headerLeft.appendChild(findLink);

  const headerRight = document.createElement('div');
  headerRight.className = 'header-right';

  const myLink = document.createElement('a');
  myLink.className = 'nav-item';
  if (loggedIn) {
    myLink.href = '/my-events.html';
    myLink.innerText = 'My Events';
  } else {
    myLink.href = '/login.html';
    myLink.innerText = 'Login';
  }
  headerRight.appendChild(myLink);

  if (loggedIn) {
    const logoutLink = document.createElement('a');
    logoutLink.className = 'nav-item';
    logoutLink.href = 'javascript:firebaseLogout()';
    logoutLink.innerText = 'Logout';
    headerRight.appendChild(logoutLink);
  }

  const dropdown = document.createElement('div');
  dropdown.className = 'dropdown';

  const iconLink = document.createElement('a');
  iconLink.className = 'icon-item';
  iconLink.href = 'javascript:void(0);';
  iconLink.setAttribute('onclick', 'toggleNavMenu()');
  const icon = document.createElement('img');
  icon.src = 'images/menu.svg';
  icon.alt = 'Menu drop down.';
  iconLink.appendChild(icon);
  dropdown.appendChild(iconLink);
  headerRight.appendChild(dropdown);

  const header = document.getElementsByClassName('header')[0];
  header.textContent = '';
  header.appendChild(headerLeft);
  header.appendChild(headerRight);

  // creates this structure:
  //   <div class="header-left">
  //     <a href="/index.html">
  //       <img src="images/uniteLogo.svg" alt="Unite by STEP logo.">
  //     </a>
  //     <a class="nav-item" href="/index.html">Home</a>
  //     <a class="nav-item" href="/create-event.html">Create</a>
  //     <a class="nav-item" href="/search.html">Find</a>
  //   </div>
  //   <div class="header-right">
  //     <a class="nav-item" href="/my-events.html">My Events</a>
  //   </div>
}

/**
 * expands and collapses details section on individual event display
 */
function toggleDetails() {
  const detailsBox = document.getElementsByClassName('event-right-details')[0];
  const arrowIcon = document.getElementById('expand-arrow');
  if (detailsBox.classList.contains('expand')) {
    detailsBox.classList.remove('expand');
    arrowIcon.src = 'images/arrow-up.svg';
    arrowIcon.alt = 'Expand';
  } else {
    detailsBox.classList.add('expand');
    arrowIcon.src = 'images/arrow-down.svg';
    arrowIcon.alt = 'Collapse';
  }
}

/**
 * Toggles the navigation menu for the mobile layout.
 */
function toggleNavMenu() {
  const exists = document.getElementById('dropdown-bar');

  if (exists != null) {
    document.getElementById('dropdown-bar').remove();
  } else {
    generateMobileNavLayout();
  }
}

/**
 * Generates the navigation menu layout on mobile.
 */
function generateMobileNavLayout() {
  const dropdownContainer = document.getElementsByClassName('dropdown')[0];

  const dropdownBar = document.createElement('div');
  dropdownBar.className = 'dropdown-bar';
  dropdownBar.id = 'dropdown-bar';

  const homeBullet = document.createElement('li');
  const homeLink = document.createElement('a');
  homeLink.className = 'dropdown-item';
  homeLink.href = '/index.html';
  homeLink.innerText = 'Home';
  homeBullet.appendChild(homeLink);
  dropdownBar.appendChild(homeBullet);

  const createBullet = document.createElement('li');
  const createLink = document.createElement('a');
  createLink.className = 'dropdown-item';
  createLink.href = '/create-event.html';
  createLink.innerText = 'Create';
  createBullet.appendChild(createLink);
  dropdownBar.appendChild(createBullet);

  const findBullet = document.createElement('li');
  const findLink = document.createElement('a');
  findLink.className = 'dropdown-item';
  findLink.href = '/search.html';
  findLink.innerText = 'Find';
  findBullet.appendChild(findLink);
  dropdownBar.appendChild(findBullet);

  const myBullet = document.createElement('li');
  const myLink = document.createElement('a');
  myLink.className = 'dropdown-item';
  if (loggedIn) {
    myLink.href = '/my-events.html';
    myLink.innerText = 'My Events';
  } else {
    myLink.href = '/login.html';
    myLink.innerText = 'Login';
  }
  myBullet.appendChild(myLink);
  dropdownBar.appendChild(myBullet);

  if (loggedIn) {
    const logoutBullet = document.createElement('li');
    const logoutLink = document.createElement('a');
    logoutLink.className = 'dropdown-item';
    logoutLink.href = 'javascript:firebaseLogout()';
    logoutLink.innerText = 'Logout';
    logoutBullet.appendChild(logoutLink);
    dropdownBar.appendChild(logoutBullet);
  }

  dropdownContainer.appendChild(dropdownBar);
}

/** *********************************************************************
 * Loading methods for event search/display -- distance settings,
 * rainbow tags, event lists
 ***********************************************************************/

// option constants to use with getEvents
const recommendedForYou = 0;
const savedEvents = 1;
const createdEvents = 2;
const searchResults = 3;

const MI_TO_KM = 1.609;

// Two test examples to use with getEvents()
const test = {eventName: 'Beach clean up',
  eventDescription: 'Lorem ipsum dolor sit amet, consectetur. ' +
      'Nam efficitur enim quis est mollis blandit. Integer vitae risus. ' +
      'Nunc sit amet semper urna, ac mollis dui. Aenean vitae imperdiet, ' +
      'sit amet mattis libero. Sed tincidunt arcu in justo...',
  date: 'Saturday, June 20, 2020',
  startTime: '1:00 PM',
  distance: '5 miles away',
  streetAddress: 'Main St',
  city: 'Venice',
  state: 'CA',
  attendeeCount: 12,
  tags: ['environment'],
  key: 'aglub19hcHBfaWRyEgsSBUV2ZW50GICAgICAgIAKDA'};
const test2 = {eventName: 'Book Drive',
  eventDescription: 'Lorem ipsum dolor sit amet, consectetur elit. ' +
      'Nam efficitur enim quis est mollis blandit. Integer vitae augue. ' +
      'Nunc sit amet semper urna, ac mollis dui. Aenean vitae nisi, ' +
      'sit amet mattis libero. Sed tincidunt arcu in justo...',
  date: 'Sunday, June 21, 2020',
  startTime: '1:00 PM',
  distance: '6 miles away',
  streetAddress: 'Main St',
  city: 'Los Angeles',
  state: 'CA',
  attendeeCount: 12,
  tags: ['education'],
  key: 'aglub19hcHBfaWRyEgsSBUV2ZW50GICAgICAgIAKDA'};
const dummyEvents = [test, test2];
const dummyText = 'Suggested for you'; // TODO: come up with variety

/**
 * Fetches events from a specific url
 *
 * Currently uses the events variable defined above
 *
 * @param {array} events The list of events to display
 * @param {number} index To identify which event list container
 *                           on the page to generate.
 * @param {number} option To identify which format to generate--
 *                           attendee count, unsave/edit event,
 *                           or a recommendation reason.
 */
async function getEvents(events, index, option) {
  const eventListElements =
      document.getElementsByClassName('event-list-container');

  if (index == null || index >= eventListElements.length) {
    index = 0;
  }

  // check which stylesheet we are currently using
  const styleLink = document.getElementById('style').href;
  const styleName = styleLink.substring(styleLink.lastIndexOf('/') + 1);
  const onMobile = (styleName.indexOf('mobile') >= 0);

  const eventListElement = eventListElements[index];
  eventListElement.innerHTML = '';

  if (events.length == 0) {
    const noElementsBox = document.createElement('div');
    noElementsBox.className = 'no-events';
    const noElementsText = document.createElement('div');
    noElementsText.className = 'no-events-text';
    if (option == savedEvents) {
      noElementsText.innerText = 'You have not saved any events yet! ' +
          'Click the ‘Find’ tab to to find an event you would like to save.';
    } else if (option == createdEvents) {
      noElementsText.innerText = 'You have not created an event yet! ' +
          'Click the ‘Create’ tab to create your first event.';
    } else if (option == searchResults) {
      noElementsText.innerText = 'No events found matching your desired tags.';
    } else {
      noElementsText.innerText = 'No events to see! Create one now.';
    }

    noElementsBox.appendChild(noElementsText);
    eventListElement.appendChild(noElementsBox);
    return;
  }

  events.forEach(function(event) {
    const eventId = event.key.id;
    event = event.propertyMap;

    const eventItemElement = document.createElement('a');
    eventItemElement.className = 'event-item';
    eventItemElement.setAttribute('onclick', 'openLink("' +
        event.eventKey + '")');
    eventListElement.appendChild(eventItemElement);

    const eventImageElement = document.createElement('div');
    let primaryTag = event.tags[0];
    if (primaryTag == 'LGBTQ+') primaryTag = 'LGBTQ';
    eventImageElement.className = 'event-image ' + primaryTag;

    const eventItemInfoElement = document.createElement('div');
    eventItemInfoElement.className = 'event-item-info';

    const eventItemHeaderElement = document.createElement('div');
    eventItemHeaderElement.className = 'event-item-header';
    eventItemInfoElement.appendChild(eventItemHeaderElement);

    const eventItemTitleElement = document.createElement('div');
    eventItemTitleElement.className = 'event-item-title';

    // show event address if on the my-events page
    if (option == createdEvents || option == savedEvents) {
      const eventItemTitleName = document.createElement('div');
      eventItemTitleName.innerText = event.eventName;
      const eventItemTitleAddr = document.createElement('div');
      eventItemTitleAddr.innerText = event.address;
      eventItemTitleAddr.className = 'event-item-address';
      eventItemTitleElement.appendChild(eventItemTitleName);
      eventItemTitleElement.appendChild(eventItemTitleAddr);
    } else {
      eventItemTitleElement.innerText = event.eventName;
    }

    const eventItemDetailsElement = document.createElement('div');
    eventItemDetailsElement.className = 'event-item-details';
    // determine order of elements depending on mobile or non-mobile layout
    if (onMobile) {
      // image is part of event-header, inside event-item-info
      // event-item-title is part of event-header, outside of
      // event-item-details
      eventItemElement.appendChild(eventItemInfoElement);
      eventItemHeaderElement.appendChild(eventItemDetailsElement);
      eventItemHeaderElement.appendChild(eventImageElement);
      eventItemDetailsElement.appendChild(eventItemTitleElement);
    } else {
      // image is outisde of event-item-info
      // event-item-title is part of event-item-details
      eventItemElement.appendChild(eventImageElement);
      eventItemElement.appendChild(eventItemInfoElement);
      eventItemHeaderElement.appendChild(eventItemTitleElement);
      eventItemHeaderElement.appendChild(eventItemDetailsElement);
    }

    const eventItemDateElement = document.createElement('div');
    eventItemDateElement.className = 'event-item-date';
    eventItemDateElement.innerText = event.date;
    eventItemDetailsElement.appendChild(eventItemDateElement);
    const eventItemDistanceElement = document.createElement('div');
    eventItemDistanceElement.className = 'event-item-distance';
    if (option == savedEvents || option == createdEvents) {
      eventItemDistanceElement.innerText = event.startTime;
    } else {
      if (event.distance != null) {
        eventItemDistanceElement.innerText =
            Math.round(event.distance / MI_TO_KM) + ' mi away';
      } else {
        eventItemDistanceElement.innerText = event.address;
      }
    }
    eventItemDetailsElement.appendChild(eventItemDistanceElement);

    const eventItemDescElement = document.createElement('div');
    eventItemDescElement.className = 'event-item-description';
    eventItemDescElement.innerText = event.eventDescription;
    eventItemInfoElement.appendChild(eventItemDescElement);

    const eventItemFooterElement = document.createElement('div');
    eventItemFooterElement.className = 'event-item-footer';
    eventItemInfoElement.appendChild(eventItemFooterElement);

    // decide which footer item to use
    const attendeeCountContainerElement = document.createElement('div');
    if (option == recommendedForYou) {
      // "recommended for you"
      attendeeCountContainerElement.innerText = dummyText;
    } else if (option == savedEvents) {
      // unsave an event
      attendeeCountContainerElement.className = 'edit-unsave-event';
      attendeeCountContainerElement.innerText = 'Unsave this event';
      attendeeCountContainerElement.onclick = function() {
        unsaveEvent(eventId);
      };
    } else if (option == createdEvents) {
      // edit an event
      attendeeCountContainerElement.className = 'edit-unsave-event';
      const editEventLink = document.createElement('a');
      editEventLink.innerText = 'Edit this event';

      editEventLink.href = '/edit-event-form.html';
      attendeeCountContainerElement.appendChild(editEventLink);
    } else {
      // default: show attendee count
      if (event.attendeeCount == null) {
        event.attendeeCount = 0;
      }

      attendeeCountContainerElement.className = 'attendee-count-container';

      const attendeeCountElement = document.createElement('span');
      attendeeCountElement.className = 'attendee-count ' + primaryTag +
          '-text';
      attendeeCountElement.innerText = event.attendeeCount;
      attendeeCountContainerElement.appendChild(attendeeCountElement);
      attendeeCountContainerElement.appendChild(
          document.createTextNode(' already attending'));
    }

    eventItemFooterElement.appendChild(attendeeCountContainerElement);

    const tagsContainerElement = document.createElement('div');
    tagsContainerElement.className = 'tags-container';
    eventItemFooterElement.appendChild(tagsContainerElement);
    event.tags.forEach(function(tag) {
      const tagElement = document.createElement('span');
      // class name is now (for example) 'tag environment'
      if (tag == 'LGBTQ+') tagElement.className = 'tag rainbow';
      else tagElement.className = 'tag ' + tag;
      tagElement.innerText = tag;
      tagsContainerElement.appendChild(tagElement);
    });
  });
  generateRainbowTags();

  // generates this structure:
  //
  //   <div class="event-item">
  //
  // DESKTOP:
  //     <div class="event-image green-background"></div>
  //     <div class="event-item-info">
  //       <div class="event-item-header">
  //         <div class="event-item-title">Beach clean up</div>
  //         <div class="event-item-details">
  //             <div>Saturday, June 20, 2020</div>
  //             <div>5 miles away</div>
  //         </div>
  //       </div>
  // MOBILE:
  //
  //     <div class="event-item-info">
  //       <div class="event-item-header">
  //         <div class="event-item-details">
  //         <div class="event-item-title">Beach clean up</div>
  //             <div>Saturday, June 20, 2020</div>
  //             <div>5 miles away</div>
  //         </div>
  //         <div class="event-image green-background"></div>
  //       </div>
  //
  //       <div class="event-item-description">
  //         Lorem ipsum dolor sit amet, consectetur adipiscing elit.
  //         Nam efficitur enim quis est mollis blandit. Integer vitae.
  //         Nunc sit amet semper urna, ac mollis dui. Aenean vitae imperdiet,
  //         sit amet mattis libero. Sed tincidunt arcu in justo...</div>
  //       <div class="event-item-footer">
  //         *this item depends on value of 'option'*
  //         <div class="attendee-count-container">
  //             <span class="attendee-count green">12</span> already attending
  //         </div>
  //         <div class="tags-container">
  //             <span class="tag green-background">environment</span>
  //         </div>
  //       </div>
  //     </div>
  //   </div>
}

/**
 * Opens an event via its key name.
 * @param {string} key web safe key string.
 */
function openLink(key) {
  getUserIDToken().then((userToken) => {
    const url = '/load-event?Event=' + key + '&userToken=' + userToken;
    window.location.href = url;
  });
}

let searchDistance = 5;

/**
 * Retrieves the search distance setting from the page.
 */
function changeSearchDistance() {
  searchDistance = document.getElementById('searchDistance').value;
  search();
}

/**
 * Creates the search distance settings on the page
 */
async function getSearchDistanceSettings() {
  getLocation().then((location) => {
    const locationSettingsElements =
    document.getElementsByClassName('location-settings');
    const locationSettingsElement = locationSettingsElements[0];

    // TODO get from server where the user's location is to set by default.
    locationSettingsElement.innerHTML = '';
    const currentLocationElement = document.createElement('div');
    currentLocationElement.className = 'current-location';
    currentLocationElement.innerText = 'Current Location: ' +
      location;
    locationSettingsElement.appendChild(currentLocationElement);
    locationSettingsElement.appendChild(
        document.createTextNode(' '));

    const changeLinkElement = document.createElement('a');
    changeLinkElement.setAttribute('href', 'javascript:changeLocation()');
    changeLinkElement.innerText = 'Change Location';
    locationSettingsElement.appendChild(changeLinkElement);

    const distanceElement = document.createElement('div');
    distanceElement.className = 'distance';
    distanceElement.appendChild(
        document.createTextNode('Searching within '));
    locationSettingsElement.appendChild(distanceElement);

    const selectElement = document.createElement('select');
    selectElement.id = 'searchDistance';
    selectElement.setAttribute('onchange', 'changeSearchDistance()');
    distanceElement.appendChild(selectElement);

    const distanceList = [5, 10, 25, 50];
    distanceList.forEach(function(distance) {
      const optionElement = document.createElement('option');
      optionElement.value = distance;
      optionElement.innerText = distance;
      if (distance == searchDistance) {
        optionElement.setAttribute('selected', 'true');
      }
      selectElement.appendChild(optionElement);
    });

    distanceElement.appendChild(
        document.createTextNode(' mi'));
  });
}

/**
 * Generates all the rainbow tags on a page, currently made to be used
 * with LGBTQ+
 *
 * To have a rainbow tag generated, set its innerText to the tag name and
 *  give it the class 'rainbow'
 */
const colors = ['#FF0900', '#FF7F00', '#FFB742', '#00F11D', '#0079FF',
  '#A800FF'];
const tagsAll = ['environment', 'blm', 'volunteer', 'education', 'LGBTQ+'];
const tagsSearch = [];
const tagsBox = [...tagsAll];
const tagsOnEvent = [];

/**
 * Generates all the rainbow tags on a page.
 */
function generateRainbowTags() {
  const elements = document.getElementsByClassName('rainbow');
  for (let e = 0; e < elements.length; e++) {
    const tag = elements[e].innerText;
    let tagHTML = '';
    let colorIndex = 0;
    for (let i = 0; i < tag.length; i++) {
      if (colorIndex >= colors.length) {
        colorIndex = 0;
      }
      tagHTML = tagHTML + '<span style="color: ' + colors[colorIndex] +
          '">' + tag.charAt(i) + '</span>';
      colorIndex++;
    }
    elements[e].innerHTML = tagHTML;
  }
}

/**
 * Sets value of a cookie.
 *
 * @param cname name of the cookie
 * @param cvalue name of the value
 */
function setCookie(cname, cvalue) {
  document.cookie = cname + '=' + cvalue + '; ';
}

/**
 * Gets value of a cookie.
 *
 * @param cname name of the cookie
 * @returns the value of the requested cookie
 */
function getCookie(cname) {
  const name = cname + "=";
  const decodedCookie = decodeURIComponent(document.cookie);
  const cArr = decodedCookie.split(';');
  for (let i = 0; i < cArr.length; i++) {
    let c = cArr[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return '';
}

/**
 * Retrieves the users location, prioritizing the datastore
 * but falling back on cookies
 *
 * @returns the location of the user
 */
function getLocation() {
  return new Promise((resolve) => {
    const locationCookie = getCookie('location');
    let location = '';
    getUserIDToken().then((userToken) => {
      fetch('/location?userToken=' + userToken)
          .then((response) => response.json())
          .then(function(js) {
            const locationDatastore = js;
            if (locationDatastore != ''
                && locationCookie != locationDatastore) {
              setCookie('location', locationDatastore);
              location = locationDatastore;
            } else {
              location = locationCookie;
            }
            resolve(location);
          });
    });
  });
}

/**
 * Prompts the user to enter their zipcode to change their location
 */
function changeLocation() {
  const location = prompt('Please enter your zipcode:', '');
  if (location == null || location == '') {
    console.log('incomplete');
  } else {
    setCookie('location', location);
    getUserIDToken().then((userToken) => {
      const params = new URLSearchParams();
      params.append('zip', location);
      params.append('userToken', userToken);
      fetch(new Request('/location', {method: 'POST', body: params}))
          .then(() => {
        getSearchDistanceSettings();
      });
    });
  }
}

/* **********************************************************************
 * Methods for create-event-form and edit-event form
 * **********************************************************************/

/* This is an array to keep track of the current form's selected tags. */
const tagsSelected = [];

/**
 * Inverts the apperance of a selected tag and adds it to the list
 * of selected tags
 * @param {String} tag the name of the tag to be toggled
 */
function toggleTagEvent(tag) {
  const boxIndex = tagsBox.indexOf(tag);
  if (boxIndex > -1) {
    tagsOnEvent[boxIndex] = !tagsOnEvent[boxIndex];

    if (tagsSelected.includes(tag)) {
      tagsSelected.splice(boxIndex);
    } else {
      tagsSelected.push(tag);
    }
  }

  updateEventTagBox();
}

/**
 * Verifies that at least one tag is selected. If not, cancel form submit
 * and display error.
 */
function verifyTags() {
  if (tagsSelected.length > 0) {
    // Convert tags selected array into string
    const jsonArray = JSON.stringify(tagsSelected);
    const tags = createHiddenInput(jsonArray);
    getUserIDToken().then((preToken) => {
      const userToken = createHiddenInput(preToken);
      userToken.name = 'userToken';

      // Add string of tags and userToken to form for submission
      document.getElementById('eventform').appendChild(tags);
      document.getElementById('eventform').appendChild(userToken);

      document.eventform.submit();
      tagsSelected.splice(0, tagsSelected.length);
    });
  } else {
    // Display error and prevent from sumbission
    const tagBoxError = document.getElementById('tags-label');
    tagBoxError.style.borderStyle = 'solid';
    tagBoxError.style.borderColor = 'red';
    event.preventDefault();
  }
}

/**
 * Creates a hidden input for the array of tags. Used to keep track
 * of which tags have been selected.
 * @param {String} jsonArray a JSON array of type string.
 * @return {input} returns the hidden input element.
 */
function createHiddenInput(jsonArray) {
  const tagsArray = document.createElement('input');
  tagsArray.setAttribute('type', 'hidden');
  tagsArray.setAttribute('name', 'all-tags');
  tagsArray.setAttribute('id', 'all-tags');
  tagsArray.setAttribute('value', jsonArray);

  return tagsArray;
}

/**
 * Updates the tag box on the event form submit page.
 */
function updateEventTagBox() {
  const elements = document.getElementsByClassName('tag-box');
  const tagBoxElement = elements[0];
  tagBoxElement.innerHTML = '';
  for (let i = 0; i < tagsBox.length; i++) {
    const tag = tagsBox[i];
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'toggleTagEvent("' + tag +
        '")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;

    // if tag is on the event, invert it
    if (tagsOnEvent[i]) {
      spanElement.className = spanElement.className + ' invert';
      spanElement.innerText = '✓' + tag;
    } else {
      spanElement.innerText = tag;
    }

    tagBoxElement.appendChild(spanElement);
  }

  generateRainbowTags();
}

/* **********************************************************************
 * Methods for index.html
 * **********************************************************************/

/**
 * Onload actions for index.html
 * Fetches events from server, calls getEvents with correct options and loads
 * Search distance options
 */
async function getRecommendedEvents() {
  if (loggedIn) {
    getUserIDToken().then((userToken) => {
      fetch('/user?get=saved&userToken=' + userToken)
          .then((response) => response.json())
          .then(function(js) {
            // TODO: change this fetch call to get recommendations instead
            getEvents(dummyEvents, 1, 0);
          });
    });
  } else {
    alert('Please log in first!');
  }
  getSearchDistanceSettings();
}


/* **********************************************************************
 *  Methods for my-events.html
 * **********************************************************************/

/**
 * On loading my-events.html, fetches events from server and calls getEvents
 * with correct options.
 */
async function getMyEvents() {
  if (loggedIn) {
    getUserIDToken().then((userToken) => {
      fetch('/user?get=saved&userToken=' + userToken)
          .then((response) => response.json())
          .then(function(js) {
            getEvents(js, 0, 1);
          });
      fetch('/user?get=created&userToken=' + userToken)
          .then((response) => response.json())
          .then(function(js) {
            getEvents(js, 1, 2);
          });
    });
  } else {
    const eventListElements =
        document.getElementsByClassName('event-list-container');

    const savedListElement = eventListElements[0];
    savedListElement.innerHTML = '';
    const savedBox = document.createElement('div');
    savedBox.className = 'no-events';
    const savedText = document.createElement('div');
    savedText.className = 'no-events-text';
    savedText.innerText = 'Please login to view your saved events!';
    savedBox.appendChild(savedText);
    savedListElement.appendChild(savedBox);

    const createdListElement = eventListElements[1];
    createdListElement.innerHTML = '';
    const createdBox = document.createElement('div');
    createdBox.className = 'no-events';
    const createdText = document.createElement('div');
    createdText.className = 'no-events-text';
    createdText.innerText = 'Please login to view your created events!';
    createdBox.appendChild(createdText);
    createdListElement.appendChild(createdBox);
  }
}

/**
 * Makes the servlet call to unsave an event.
 *
 * @param {number} eventId Id of the event to be unsaved.
 */
async function unsaveEvent(eventId) {
  getUserIDToken().then((userToken) => {
    const params = new URLSearchParams();
    params.append('event', eventId);
    params.append('action', 'unsave');
    params.append('userToken', userToken);
    fetch(new Request('/user', {method: 'POST', body: params})).then(() => {
      window.location.href = '/my-events.html';
    });
  });
}

/* **********************************************************************
 * Methods for search.html
 * **********************************************************************/

/**
 * Load all actions for the search page.
 */
function searchLoadActions() {
  updateSearchBar();
  updateTagBox();
  getSearchDistanceSettings();
}

/**
 * Add the selected tag to the search bar and remove it from the tag box.
 * @param {String} tag the name of the tag to add.
 */
function addTagBoxToSearch(tag) {
  const boxIndex = tagsBox.indexOf(tag);
  if (boxIndex > -1) {
    tagsBox.splice(boxIndex, 1);
  }

  tagsSearch.push(tag);
  updateTagBox();
  updateSearchBar();
}

/**
 * Adds selected tag to the tag box.
 * @param {String} tag the name of the tag to add.
 */
function addTagSearchToBox(tag) {
  const searchIndex = tagsSearch.indexOf(tag);
  if (searchIndex > -1) {
    tagsSearch.splice(searchIndex, 1);
  }

  tagsBox.splice(tagsAll.indexOf(tag), 0, tag);
  updateSearchBar();
  updateTagBox();
}

/**
 * Update the tags shown in the search bar.
 */
function updateSearchBar() {
  const elements = document.getElementsByClassName('search-bar');
  const searchBarElement = elements[0];
  searchBarElement.innerHTML = '';
  tagsSearch.forEach(function(tag) {
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'addTagSearchToBox("' + tag +
        '")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;
    spanElement.innerText = tag;

    searchBarElement.appendChild(spanElement);
  });

  generateRainbowTags();
}

/**
 * Update the tags shown in the tag box.
 */
function updateTagBox() {
  const elements = document.getElementsByClassName('tag-box');
  const tagBoxElement = elements[0];
  tagBoxElement.innerHTML = '';
  tagsBox.forEach(function(tag) {
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'addTagBoxToSearch("' + tag +
        '")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;
    spanElement.innerText = tag;

    tagBoxElement.appendChild(spanElement);
  });

  generateRainbowTags();
}

/**
 * Placeholder function for search functionality
 */
function search() {
  let url = '?tags=';
  tagsSearch.forEach(function(tag) {
    if (tag == 'LGBTQ+') {
      url += 'LGBTQ%2B' + ',';
    } else {
      url += tag + ',';
    }
  });
  // trim the last comma off
  if (url.charAt(url.length - 1) == ',') {
    url = url.substring(0, url.length - 1);
  }

  getLocation().then((location) => {
    // TODO get user location
    url += '&searchDistance=' + searchDistance + '&location=' + location;

    fetch('/search' + url).then((response) => response.json())
        .then(function(responseJson) {
          getEvents(responseJson, 0, 3);
        });
  });
}

/* **********************************************************************
 * Methods for survey.html
 * **********************************************************************/

const surveyResponses = [-1, -1, -1, -1, -1];

/**
 * Toggles the display of the survey page to indicate which option is selected,
 * while saving the survey responses.
 *
 * @param {number} question The question number to apply the toggle.
 * @param {number} index Which bubble on the question to apply the toggle to.
 */
function toggleSurveyDisplay(question, index) {
  const circle =
    document.getElementsByClassName('survey-select')[question*5 + index];
  if (surveyResponses[question] >= 0) {
    const oldIndex = surveyResponses[question];
    const oldCircle =
      document.getElementsByClassName('survey-select')[question*5 + oldIndex];
    oldCircle.style.backgroundColor = 'transparent';
  }
  circle.style.backgroundColor = 'white';
  surveyResponses[question] = index;
}

/**
 * Verifies the survey is completed. If it is completed, submits.
 */
function submitSurvey() {
  if (loggedIn) {
    const params = new URLSearchParams();
    for (let i = 0; i < surveyResponses.length; i++) {
      const score = surveyResponses[i];
      if (score < 0) {
        // TODO: streamline this probably
        alert('Please finish the survey first!');
        return;
      } else {
        params.append(tagsAll[i], score);
      }
    }
    params.append('userToken', getUserIDToken());
    fetch(new Request('/submit-survey', {method: 'POST', body: params}));
  } else {
    // cannot submit while not logged in
    alert('Please log in first!');
  }
}

/* **********************************************************************
 * Methods for display-event.jsp
 * **********************************************************************/

/**
 * Generates tags and tag-based features.
 *
 * @param {number} id Id of the event to be displayed.
 * @param {number} alreadySaved Status code for if event has
 *                  already been saved by the user.
 */
function displayIndividualEvent(id = 0, alreadySaved = -1) {
  const tagString = document.getElementById('tags-value').value;
  const tagArray = JSON.parse(tagString);
  let mainColor = tagArray[0];

  if (mainColor == 'LGBTQ+') {
    mainColor = 'LGBTQ';
  }

  loadEventTags(tagArray);
  loadDefaultImage(mainColor);
  loadAttendingColor(mainColor);
  loadOptionalFields();
  loadLinks();
  setupSave(id, alreadySaved);
}

/**
 * Displays the event tags.
 * @param {array} events the array of event tags
 */
function loadEventTags(events) {
  const tagsContainer = document.getElementsByClassName('tags-container')[0];

  for (let i = 0; i < events.length; i++) {
    const tag = document.createElement('span');
    let tagType = events[i];
    tag.innerHTML = tagType;

    if (tagType == 'LGBTQ+') {
      tagType = 'rainbow';
    }

    tag.className = 'tag ' + tagType;
    tagsContainer.appendChild(tag);
  }
  generateRainbowTags();
}

/**
 * Displays the default image based on the color of the first tag.
 * @param {String} color the color of the primary event.
 */
function loadDefaultImage(color) {
  const eventImage = document.getElementsByClassName('event-display-image')[0];
  eventImage.className = 'event-display-image ' + color;
}

/**
 * Displays the 'amount attending' text based on the color of the first tag.
 * @param {String} color the color of the primary event.
 */
function loadAttendingColor(color) {
  const countContainer = document.getElementsByClassName('attendee-count')[0];
  countContainer.className = 'attendee-count ' + color + '-text';
}

/* **********************************************************************
 * Methods for login.html
 * **********************************************************************/

// FirebaseUI config.
const uiConfig = {
  // TODO: replace with URL user came from each time?
  signInSuccessUrl: '/index.html',
  signInOptions: [
    firebase.auth.GoogleAuthProvider.PROVIDER_ID,
    firebase.auth.EmailAuthProvider.PROVIDER_ID,
    firebase.auth.FacebookAuthProvider.PROVIDER_ID,
  ],
  // tosUrl and privacyPolicyUrl accept either url string or a callback
  // function.
  // Terms of service url/callback.
  tosUrl: '<your-tos-url>',
  // Privacy policy url/callback.
  privacyPolicyUrl: function() {
    window.location.assign('<your-privacy-policy-url>');
  },
};

/**
 * Displays the Firebase login ui on the login page
 */
function initializeFirebaseLogin() {
  const ui = new firebaseui.auth.AuthUI(firebase.auth());
  ui.start('#firebaseui-auth-container', uiConfig);
}

/**
 * Loads optional field end time.
 */
function loadOptionalFields() {
  const endTime = document.getElementById('end-value').value;

  if (endTime != '') {
    const timeContainer = document.getElementsByClassName('time')[0];
    const end = document.createElement('p');
    end.innerHTML = 'End time: ' + endTime;

    timeContainer.appendChild(end);
  }
}

/**
 * Generates share links for Facebook, Twitter, and mail.
 */
function loadLinks() {
  const eventKey = document.getElementById('event-key').value;
  const twitter = document.getElementById('twitter-link');
  const facebook = document.getElementById('fb-link');
  const mail = document.getElementById('mail-link');
  const url = 'http://unitebystep.appspot.com/load-event?Event=' + eventKey;
  const br = '%0D%0A%0D%0A';

  twitter.href = 'https://twitter.com/share?url=' + url;
  facebook.href = 'http://www.facebook.com/sharer.php?u=' + url;
  mail.href =
   'mailto:?subject=Unite by STEP Event&body=Check out this event!' + br + url;
}

/**
 * Adds onclick action to the event display's save-event button.
 * @param {number} id Id of the event to be saved/unsaved.
 * @param {number} alreadySaved Status code for if event has
 *                  already been saved by the user.
 */
function setupSave(id, alreadySaved) {
  const saveButton = document.getElementsByClassName('save-event')[0];
  if (alreadySaved >= 0) {
    saveButton.innerText = 'Unsave Event';
    saveButton.onclick = function() {
      unsaveEvent(id);
    };
  } else {
    saveButton.onclick = function() {
      saveEvent(id);
    };
  }
}

/**
 * Makes the servlet call to save an event.
 *
 * @param {number} eventId Id of the event to be saved.
 */
function saveEvent(eventId) {
  getUserIDToken().then((userToken) => {
    const params = new URLSearchParams();
    params.append('event', eventId);
    params.append('action', 'save');
    params.append('userToken', userToken);
    fetch(new Request('/user', {method: 'POST', body: params})).then(() => {
      window.location.href = '/my-events.html';
    });
  });
}
