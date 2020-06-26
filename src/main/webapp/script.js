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

/* determines which stylesheet to use and generates nav bar*/
function loadActions() {
  const styleSheetElement = document.getElementById('style');
  if (window.innerWidth >= window.innerHeight) {
    styleSheetElement.href = '/style.css';
  } else {
    styleSheetElement.href = 'style-mobile.css';
  }

  generateNavBar();
}

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
  myLink.href = '/my-events.html';
  myLink.innerText = 'My Events';
  headerRight.appendChild(myLink);

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

/* expands and collapses details section on individual event display */
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

var tagsAll = ['environment', 'blm', 'volunteer', 'education', 'LGBTQ+'];
var tagsSearch = [];
var tagsBox = [...tagsAll];
var tagsOnEvent = [];

function addTagBoxToSearch(tag) {
  var boxIndex = tagsBox.indexOf(tag);
  if (boxIndex > -1) {
    tagsBox.splice(boxIndex, 1);
  }
  
  tagsSearch.push(tag);
  updateTagBox();
  updateSearchBar();
}

function addTagSearchToBox(tag) {
  var searchIndex = tagsSearch.indexOf(tag);
  if (searchIndex > -1) {
    tagsSearch.splice(searchIndex, 1);
  }
  
  tagsBox.splice(tagsAll.indexOf(tag), 0, tag);
  updateSearchBar();
  updateTagBox();
}

function toggleTagEvent(tag) {
  var boxIndex = tagsBox.indexOf(tag);
  if (boxIndex > -1) {
    tagsOnEvent[boxIndex] = !tagsOnEvent[boxIndex];
  }

  updateEventTagBox();
}

function updateSearchBar() {
  const elements = document.getElementsByClassName('search-bar');
  const searchBarElement = elements[0];
  searchBarElement.innerHTML = '';
  tagsSearch.forEach(function(tag) {
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'addTagSearchToBox(\"' + tag + 
        '\")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;
    spanElement.innerText = tag;

    searchBarElement.appendChild(spanElement);
  });

  generateRainbowTags();
}

function updateTagBox() {
  const elements = document.getElementsByClassName('tag-box');
  const tagBoxElement = elements[0];
  tagBoxElement.innerHTML = '';
  tagsBox.forEach(function(tag) {
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'addTagBoxToSearch(\"' + tag + 
        '\")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;
    spanElement.innerText = tag;

    tagBoxElement.appendChild(spanElement);
  });

  generateRainbowTags();
}

function updateEventTagBox() {
  const elements = document.getElementsByClassName('tag-box');
  const tagBoxElement = elements[0];
  tagBoxElement.innerHTML = '';
  for (var i = 0; i < tagsBox.length; i++) {
    var tag = tagsBox[i];
    const spanElement = document.createElement('span');
    spanElement.setAttribute('onclick', 'toggleTagEvent(\"' + tag + 
        '\")');
    // class name is now (for example) 'tag environment'
    if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
    else spanElement.className = 'tag ' + tag;

    // if tag is on the event, invert it
    if (tagsOnEvent[i]) {
      spanElement.className = spanElement.className + ' invert';
      spanElement.innerText = '✓' + tag;
    }
    else spanElement.innerText = tag;

    tagBoxElement.appendChild(spanElement);
  }

  generateRainbowTags();
}

/**
 * Generates all the rainbow tags on a page, currently made to be used with LGBTQ+
 *
 * To have a rainbow tag generated, set its innerText to the tag name and give it
 * the class 'rainbow'
 */
const colors = ['#FF0900', '#FF7F00', '#ffe600','#00F11D', '#0079FF', '#A800FF'];
function generateRainbowTags() {
  const elements = document.getElementsByClassName('rainbow');
  for (var e = 0; e < elements.length; e++) {
    var tag = elements[e].innerText;
    var tagHTML = '';
    var colorIndex = 0;
    for (var i = 0; i < tag.length; i++) {
      if (colorIndex >= colors.length) {
        colorIndex = 0;
      }
      tagHTML = tagHTML + '<span style=\"color: ' + colors[colorIndex] + '\">' + tag.charAt(i) + '</span>';
      colorIndex++;
    }
    elements[e].innerHTML = tagHTML;
  }
}

var searchDistance = 5;

function changeSearchDistance() {
  searchDistance = document.getElementById('searchDistance').value;
  search();
}

/**
 * Placeholder function for search functionality
 */
function search() {
  var url = '/search.html?tags=';
  tagsSearch.forEach(function(tag) {
    url += tag + ',';
  });
  // trim the last comma off
  if (url.charAt(url.length - 1) == ',') {
    url = url.substring(0, url.length - 1);
  }

  url += '&searchDistance=' + searchDistance;

  window.location.href = url;
  //TODO fetch call to server with search parameters
}

/* Two test examples to use with getEvents() */
var test = {title:'Beach clean up', 
            description:'Lorem ipsum dolor sit amet, consectetur adipiscing elit. ' +
                'Nam efficitur enim quis est mollis blandit. Integer vitae augue risus. ' +
                'Nunc sit amet semper urna, ac mollis dui. Aenean vitae imperdiet nisi, ' +
                'sit amet mattis libero. Sed tincidunt arcu in justo...',
            date:'Saturday, June 20, 2020', 
            time:'1:00 PM',
            distance:'5 miles away', 
            address:'Main St, Venice, CA',
            attendeeCount: 12,
            tags:['environment'],
            url:'/display-event.html'};
var test2 = {title:'Book Drive', 
            description:'Lorem ipsum dolor sit amet, consectetur adipiscing elit. ' +
                'Nam efficitur enim quis est mollis blandit. Integer vitae augue risus. ' +
                'Nunc sit amet semper urna, ac mollis dui. Aenean vitae imperdiet nisi, ' +
                'sit amet mattis libero. Sed tincidunt arcu in justo...',
            date:'Sunday, June 21, 2020', 
            time:'1:00 PM',
            distance:'6 miles away', 
            address:'Main St, Los Angeles, CA',
            attendeeCount: 12,
            tags:['education'],
            url:'/display-event.html'};
var events = [test, test2];

const dummyText = "Suggested for you"; // TODO: come up with variety
/**
 * Fetches events from a specific url
 * 
 * Currently uses the events variable defined above
 *
 * @param {string} url TODO - what is this for?
 * @param {number} index To identify which event list container on the page to generate.
 * @param {number} option To identify which format to generate -- attendee count, 
 *		unsave/edit event, or a recommendation reason.
 */
async function getEvents(url, index, option) {
  const eventListElements = document.getElementsByClassName('event-list-container');
  url = '/display-event.html'; // use this for now
  if (index === null || index >= eventListElements.length) {
    index = 0;
  }

  // check which stylesheet we are currently using
  const styleLink = document.getElementById('style').href;
  const styleName = styleLink.substring(styleLink.lastIndexOf('/') + 1);
  var onMobile = (styleName.indexOf('mobile') >= 0);

  var eventListElement = eventListElements[index];
  eventListElement.innerHTML = '';
  events.forEach(function(event) {
    const eventItemElement = document.createElement('a');
    eventItemElement.className = 'event-item';
    eventItemElement.setAttribute('onclick', 'openEvent(\"' 
        + event.url + '\")');
    eventListElement.appendChild(eventItemElement);

    const eventImageElement = document.createElement('div');
    eventImageElement.className = 'event-image ' + event.tags[0];

    const eventItemInfoElement = document.createElement('div');
    eventItemInfoElement.className = 'event-item-info';

    const eventItemHeaderElement = document.createElement('div');
    eventItemHeaderElement.className = 'event-item-header';
    eventItemInfoElement.appendChild(eventItemHeaderElement);

    const eventItemTitleElement = document.createElement('div');
    eventItemTitleElement.className = 'event-item-title';

    // show event address if on the my-events page
    if (option == 1 || option == 2) {
      const eventItemTitleName = document.createElement('div');
      eventItemTitleName.innerText = event.title;
      const eventItemTitleAddr = document.createElement('div');
      eventItemTitleAddr.innerText = event.address;
      eventItemTitleAddr.className = 'event-item-address';
      eventItemTitleElement.appendChild(eventItemTitleName);
      eventItemTitleElement.appendChild(eventItemTitleAddr);
    } else {
      eventItemTitleElement.innerText = event.title;
    }

    const eventItemDetailsElement = document.createElement('div');
    eventItemDetailsElement.className = 'event-item-details';
    // determine order of elements depending on mobile or non-mobile layout
    if (onMobile) {
      // image is part of event-header, inside event-item-info
      // event-item-title is part of event-header, outside of event-item-details      
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
    if (option == 1 || option == 2) {
      eventItemDistanceElement.innerText = event.time;
    } else {
      eventItemDistanceElement.innerText = event.distance;
    }
    eventItemDetailsElement.appendChild(eventItemDistanceElement);

    const eventItemDescElement = document.createElement('div');
    eventItemDescElement.className = 'event-item-description';
    eventItemDescElement.innerText = event.description;
    eventItemInfoElement.appendChild(eventItemDescElement);

    const eventItemFooterElement = document.createElement('div');
    eventItemFooterElement.className = 'event-item-footer';
    eventItemInfoElement.appendChild(eventItemFooterElement);

    // decide which footer item to use
    const attendeeCountContainerElement = document.createElement('div');
    if (option == 0) {
      // "recommended for you"
      attendeeCountContainerElement.innerText = dummyText;

    } else if (option == 1) {
      // unsave an event
      attendeeCountContainerElement.className = 'edit-unsave-event';
      attendeeCountContainerElement.innerText = "Unsave this event";
      attendeeCountContainerElement.onclick = function() {
          // TODO: unsave from datastore or make popup that confirms choice first
      }
    } else if (option == 2) {
      // edit an event
      attendeeCountContainerElement.className = 'edit-unsave-event';
      const editEventLink = document.createElement("a");
      editEventLink.innerText = "Edit this event";

      editEventLink.href = "/edit-event-form.html";
      attendeeCountContainerElement.appendChild(editEventLink);
    } else {
      // default: show attendee count
      attendeeCountContainerElement.className = 'attendee-count-container';
    
      const attendeeCountElement = document.createElement('span');
      attendeeCountElement.className = 'attendee-count ' + event.tags[0] + '-text';
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
      if (tag == 'LGBTQ+') spanElement.className = 'tag rainbow';
      else tagElement.className = 'tag ' + tag;
      tagElement.innerText = tag;

      tagsContainerElement.appendChild(tagElement);
    });
  });

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
  //         Nam efficitur enim quis est mollis blandit. Integer vitae augue risus. 
  //         Nunc sit amet semper urna, ac mollis dui. Aenean vitae imperdiet nisi, 
  //         sit amet mattis libero. Sed tincidunt arcu in justo...</div>
  //       <div class="event-item-footer">
  //         <div class="attendee-count-container">     *this item depends on value of 'option'
  //             <span class="attendee-count green">12</span> already attending
  //         </div>
  //         <div class="tags-container">
  //             <span class="tag green-background">environment</span>
  //         </div>
  //       </div>
  //     </div>
  //   </div>
}

function openEvent(url) {
  window.location.pathname = url;
}

/**
 * Creates the search distance settings on the page
 */
async function getSearchDistanceSettings() {
  const locationSettingsElements = document.getElementsByClassName('location-settings');
  var locationSettingsElement = locationSettingsElements[0];

  //TODO get from server where the user's location is to set by default
  locationSettingsElement.innerHTML = '';
  const currentLocationElement = document.createElement('div');
  currentLocationElement.className = 'current-location';
  currentLocationElement.innerText = 'Current Location: ' 
      + 'Los Angeles, CA';
  locationSettingsElement.appendChild(currentLocationElement);
  locationSettingsElement.appendChild(
        document.createTextNode(' '));

  const changeLinkElement = document.createElement('a');
  changeLinkElement.setAttribute('href', '');
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

  var distanceList = [5, 10, 25, 50];
  distanceList.forEach(function(distance) {
    const optionElement = document.createElement('option');
    optionElement.value = distance;
    optionElement.innerText = distance;
    if (distance == searchDistance) optionElement.setAttribute('selected', 'true');
    selectElement.appendChild(optionElement);
  });

  distanceElement.appendChild(
        document.createTextNode(' mi'));
}

/**
 * Toggles the display of the survey page to indicate which option is selected,
 * while saving the survey responses.
 *
 * @param {number} question The question number to apply the toggle.
 * @param {number} index Which bubble on the question to apply the toggle to.
 */
var surveyResponses = [-1, -1, -1, -1, -1];
function toggleSurveyDisplay(question, index) {
	const circle = document.getElementsByClassName('survey-select')[question*5 + index];
	if (surveyResponses[question] >= 0) {
		var oldIndex = surveyResponses[question];
		const oldCircle = document.getElementsByClassName('survey-select')[question*5 + oldIndex];
		oldCircle.style.backgroundColor = 'transparent';
	}
	circle.style.backgroundColor = 'white';
	surveyResponses[question] = index;
}
