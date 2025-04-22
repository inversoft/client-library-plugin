/*
* Copyright (c) 2019-2023, FusionAuth, All Rights Reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific
* language governing permissions and limitations under the License.
*/


package fusionauth

import(
  "fmt"
  "testing"
)


func Test_DebugImplementsStringer(t *testing.T) {
  var enum interface{} = Debug("Test")
  if _, ok := enum.(fmt.Stringer); !ok {
    t.Errorf("Debug does not implement stringer interface\n")
  }
}

func Test_ErrorImplementsStringer(t *testing.T) {
  var enum interface{} = Error("Test")
  if _, ok := enum.(fmt.Stringer); !ok {
    t.Errorf("Error does not implement stringer interface\n")
  }
}

func Test_ErrorsImplementsStringer(t *testing.T) {
  var enum interface{} = Errors("Test")
  if _, ok := enum.(fmt.Stringer); !ok {
    t.Errorf("Errors does not implement stringer interface\n")
  }
}

func Test_TraceImplementsStringer(t *testing.T) {
  var enum interface{} = Trace("Test")
  if _, ok := enum.(fmt.Stringer); !ok {
    t.Errorf("Trace does not implement stringer interface\n")
  }
}

func Test_WarningImplementsStringer(t *testing.T) {
  var enum interface{} = Warning("Test")
  if _, ok := enum.(fmt.Stringer); !ok {
    t.Errorf("Warning does not implement stringer interface\n")
  }
}
