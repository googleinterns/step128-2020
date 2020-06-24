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
  header.textContent = "";
  header.appendChild(headerLeft);
  header.appendChild(headerRight);

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
            tags:['environment']};
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
            tags:['education']};
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
  url = "/display-event.html"; // use this for now
  if (index === null || index >= eventListElements.length) {
    index = 0;
  }
  var eventListElement = eventListElements[index];
  eventListElement.innerHTML = '';
  console.log(eventListElement);
  events.forEach(function(event) {
    const eventItemElement = document.createElement('a');
    eventItemElement.className = 'event-item';
    eventItemElement.setAttribute('onclick', 'openEvent()');
    eventItemElement.href = url;
    eventListElement.appendChild(eventItemElement);

    const eventImageElement = document.createElement('div');
    eventImageElement.className = 'event-image ' + event.tags[0];
    eventItemElement.appendChild(eventImageElement);

    const eventItemInfoElement = document.createElement('div');
    eventItemInfoElement.className = 'event-item-info';
    eventItemElement.appendChild(eventItemInfoElement);

    const eventItemHeaderElement = document.createElement('div');
    eventItemHeaderElement.className = 'event-item-header';
    eventItemInfoElement.appendChild(eventItemHeaderElement);

    const eventItemTitleElement = document.createElement('div');
    eventItemTitleElement.className = 'event-item-title';
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
    eventItemHeaderElement.appendChild(eventItemTitleElement);

    const eventItemDetailsElement = document.createElement('div');
    eventItemDetailsElement.className = 'event-item-details';
    eventItemHeaderElement.appendChild(eventItemDetailsElement);
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
}

function openEvent() {
  window.location.pathname = '/display-event.html'
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
