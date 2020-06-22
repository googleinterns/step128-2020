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
var tagsBar = [...tagsAll];

function addTagBarToSearch(tag) {
  var barIndex = tagsBar.indexOf(tag);
  if (index > -1) {
    tagsBar.splice(index, 1);
  }
  
  tagsSearch.push(tag);
}

function addTagSearchToBar(tag) {
  var searchIndex = tagsSearch.indexOf(tag);
  if (index > -1) {
    tagsSearch.splice(index, 1);
  }
  
  tagsBar.splice(tagsAll.indexOf(tag), 0, tag);
}

function updateSearchBar() {
}

function updateTagBar() {
}
