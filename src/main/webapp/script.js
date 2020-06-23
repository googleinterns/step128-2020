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

var tagsAll = ['environment', 'blm', 'volunteer', 'education', 'LGBTQ+'];
var tagsSearch = [];
var tagsBox = [...tagsAll];

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

/**
 * Generates all the rainbow tags on a page, currently made to be used with LGBTQ+
 *
 * To have a rainbow tag generated, set its innerText to the tag name and give it
 * the class 'rainbow'
 */
function generateRainbowTags() {
  const elements = document.getElementsByClassName('rainbow');
  for (var e = 0; e < elements.length; e++) {
    var tag = elements[e].innerText;
    var tagHTML = '';
    var colors = ['#FF0900', '#FF7F00', '#ffe600','#00F11D', '#0079FF', '#A800FF'];
    for (var i = 0; i < tag.length; i++) {
      if (i >= colors.length) break;
      tagHTML = tagHTML + '<span style=\"color: ' + colors[i] + '\">' + tag.charAt(i) + '</span>';
    }
    elements[e].innerHTML = tagHTML;
  }
}
