# viadee Process Application Validator (vPAV)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.viadee/viadeeProcessApplicationValidator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.viadee/viadeeProcessApplicationValidator) 

The tool checks Camunda projects for consistency and discovers errors in process-driven applications.
Called as JUnit test, it discovers esp. inconsistencies of a given BPMN model in the classpath and the sourcecode of an underlying java project, such as a delegate reference to a non-existing java class or a non-existing Spring bean.

A list of the consistency checks can be found [here](Checker/index.md).

To get started with the viadee Process Application Validator read [Installation and Usage](https://viadee.github.io/vPAV/InstallationUsage.html) and [Output](https://viadee.github.io/vPAV/Output.html).
We forked the [Camunda BPM examples](https://github.com/viadee/camunda-bpm-examples/) to demonstrate the easy integration of vPAV.

We recommend to integrate the consistency check in your CI builds - you can't find these inconsistencies early enough.

## Release Notes
You can find our release notes over [here](https://github.com/viadee/vPAV/blob/development/docs/ReleaseNotes.md).

## Commitments
This library will remain under an open source licence indefinately.

We follow the [semantic versioning](http://semver.org) scheme (2.0.0).

In the sense of semantic versioning, the resulting XML and JSON outputs are the _only public API_ provided here. 
We will keep these as stable as possible, in order to enable users to analyse and integrate results into the toolsets of their choice.

## Cooperation
Feel free to report issues, questions, ideas or patches. We are looking forward to it.

## Resources
Status of the development branch: [![Build Status](https://travis-ci.org/viadee/vPAV.svg?branch=development)](https://travis-ci.org/viadee/vPAV)

- The core ideas of the tool are summarized in the [2017 Blogpost](https://camunda.com/blog/2017/10/viadeeprocessapplicationvalidator/)
- Konrad Schneid, Herbert Kuchen, Sebastian Thöne, and Sascha Di Bernardo. 2021. [Uncovering data-flow anomalies in BPMN-based process-driven applications](https://dl.acm.org/doi/10.1145/3412841.3442025). In <i>Proceedings of the 36th Annual ACM Symposium on Applied Computing</i> (<i>SAC '21</i>). Association for Computing Machinery, New York, NY, USA, 1504–1512. DOI:https://doi.org/10.1145/3412841.3442025
- Konrad Schneid, Sascha Di Bernardo, Herbert Kuchen, Sebastian Thone: [Data-Flow Analysis of BPMN-Based Process-Driven Applications: Detecting Anomalies across Model and Code](https://www.ercis.org/sites/ercis/files/structure/network/research/ercis-working-papers/ercis_wp_38.pdf). In: Working Papers, European Research Center for Information Systems No. 38. Eds.: Becker, J. et al. Münster, 2021.

## Licenses
All licenses of reused components can be found on the [maven site](http://rawgit.com/viadee/vPAV/master/docs/MavenSite/project-info.html)

</br> Additionally we use the following third-party dependencies, that are not covered via maven-found licences:
- [BPMN.io](https://bpmn.io/license/) tool under the bpmn.io license. 
- [Bootstrap](https://github.com/twbs/bootstrap/blob/v4-dev/LICENSE) licensed under MIT
- [jQuery](https://jquery.org/license/) licensed under MIT
- [PopperJS](https://github.com/FezVrasta/popper.js/blob/master/LICENSE.md) licensed under MIT
- [Font Awesome](https://fontawesome.com/license/free) licensed under CC, SIL and MIT
- [AdminLTE](https://github.com/ColorlibHQ/AdminLTE) licensed under MIT
- [jQuery Knob](https://github.com/aterrien/jQuery-Knob) licensed under MIT
- [Bootstrap Table](https://github.com/wenzhixin/bootstrap-table) licensed under MIT
- [jquery-treegrid](https://github.com/maxazan/jquery-treegrid) licensed under MIT
- [FileSaver](https://github.com/eligrey/FileSaver.js) licensed under MIT
- [Start Bootstrap - Simple Sidebar](https://github.com/StartBootstrap/startbootstrap-simple-sidebar) licensed under MIT
- [Soot](https://github.com/Sable/soot) licensed under a LGPL 2.1  license

Soot and bpmn.io provide the basis for the two most exciting features of the validator, i.e. finding inconsistencies in the data and control flow across model and code an the visualization thereof. We would like to explicitly thank these two communities for their continued effort.

**BSD 3-Clause License** <br/>

Copyright (c) 2019, viadee IT-Unternehmensberatung AG
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
