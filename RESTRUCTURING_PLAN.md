# CODE RESTRUCTURING EXECUTION PLAN

## Overview
This document contains the complete incremental plan to restructure the app/ folder following Android best practices.

## Status
- Created backup branch
- Awaiting user confirmation to proceed

---

## PHASE 1: CREATE NEW PACKAGE STRUCTURE ✅

### Step 1.1: Create New Directories
```bash
mkdir -p app/src/main/java/com/soerjo/myndicam/core/manager
mkdir -p app/src/main/java/com/soerjo/myndicam/core/util/conversion
mkdir -p app/src/main/java/com/soerjo/myndicam/data/camera/internal
mkdir -p app/src/main/java/com/soerjo/myndicam/data/camera/usb
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/internal
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/usb
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/overlay
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/controls
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/dialogs
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/camera
mkdir -p app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/common
```

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 1.1: Create new package structure"

---

## PHASE 2: EXTRACT MANAGERS FROM VIEWMODEL

### Step 2.1: Create NdiManager
**File**: `app/src/main/java/com/soerjo/myndicam/core/manager/NdiManager.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 2.1: Create NdiManager"

### Step 2.2: Create UsbCameraManager
**File**: `app/src/main/java/com/soerjo/myndicam/core/manager/UsbCameraManager.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 2.2: Create UsbCameraManager"

### Step 2.3: Create Common CameraState
**File**: `app/src/main/java/com/soerjo/myndicam/data/camera/CameraState.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 2.3: Create common CameraState"

### Step 2.4: Refactor CameraViewModel (First Pass)
- Remove NDI logic from ViewModel
- Remove USB controller management from ViewModel
- Add manager instances
- Simplify initialization

**Build Check**: `./gradlew assembleDebug`
**Fix**: Update all calls to use managers
**Commit**: "Phase 2.4: Refactor CameraViewModel to use managers"

---

## PHASE 3: MOVE CONVERSION LOGIC TO CORE/UTIL

### Step 3.1: Create Nv21Converter
**File**: `app/src/main/java/com/soerjo/myndicam/core/util/conversion/Nv21Converter.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.1: Create Nv21Converter"

### Step 3.2: Create RgbaConverter
**File**: `app/src/main/java/com/soerjo/myndicam/core/util/conversion/RgbaConverter.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.2: Create RgbaConverter"

### Step 3.3: Create Yuv420Converter
**File**: `app/src/main/java/com/soerjo/myndicam/core/util/conversion/Yuv420Converter.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.3: Create Yuv420Converter"

### Step 3.4: Create Main FrameConverter
**File**: `app/src/main/java/com/soerjo/myndicam/core/util/conversion/FrameConverter.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.4: Create main FrameConverter"

### Step 3.5: Update Imports and Delete Old FrameConverter
- Find all files importing old FrameConverter
- Update imports to new location
- Delete old FrameConverter.kt

**Files to update**:
- CameraScreen.kt
- UsbCameraScreen.kt

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.5: Update imports and delete old FrameConverter"

### Step 3.6: Update UsbCameraController
- Remove convertNv21ToNv12 method
- Update imports to use new converter
- Use Nv21Converter.convertNv21ToNv12()

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 3.6: Update UsbCameraController to use new converters"

---

## PHASE 4: CREATE CAMERA HELPER

### Step 4.1: Create CameraHelper
**File**: `app/src/main/java/com/soerjo/myndicam/data/camera/internal/CameraHelper.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 4.1: Create CameraHelper"

### Step 4.2: Move InternalCameraController to internal/
```bash
mv app/src/main/java/com/soerjo/myndicam/data/camera/InternalCameraController.kt \
   app/src/main/java/com/soerjo/myndicam/data/camera/internal/InternalCameraController.kt
```
- Update package declaration
- Use CameraHelper in InternalCameraController

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 4.2: Move InternalCameraController to internal/ subpackage"

### Step 4.3: Move UsbCameraController to usb/
```bash
mv app/src/main/java/com/soerjo/myndicam/data/camera/UsbCameraController.kt \
   app/src/main/java/com/soerjo/myndicam/data/camera/usb/UsbCameraController.kt
```
- Update package declaration
- Update CameraState import

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 4.3: Move UsbCameraController to usb/ subpackage"

---

## PHASE 5: EXTRACT UI COMPONENTS

### Step 5.1: Extract Camera Preview View
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/camera/CameraPreviewView.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.1: Extract CameraPreviewView component"

### Step 5.2: Extract Overlay Components

#### 5.2a: Create LiveBadge
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/overlay/LiveBadge.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.2a: Extract LiveBadge component"

#### 5.2b: Create FpsResolutionInfoBox
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/overlay/FpsResolutionInfoBox.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.2b: Extract FpsResolutionInfoBox component"

#### 5.2c: Create TallyBorder
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/overlay/TallyBorder.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.2c: Extract TallyBorder component"

#### 5.2d: Create NoUsbCameraOverlay
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/overlay/NoUsbCameraOverlay.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.2d: Extract NoUsbCameraOverlay component"

### Step 5.3: Extract Control Components

#### 5.3a: Create StreamToggleFAB
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/controls/StreamToggleFAB.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.3a: Extract StreamToggleFAB component"

#### 5.3b: Create CircularIconButton
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/controls/CircularIconButton.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.3b: Extract CircularIconButton component"

#### 5.3c: Create CameraSwitchButton
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/controls/CameraSwitchButton.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.3c: Extract CameraSwitchButton component"

### Step 5.4: Extract Dialog Components

#### 5.4a: Create MenuDialog
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/dialogs/MenuDialog.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.4a: Extract MenuDialog component"

#### 5.4b: Create SettingsDialog
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/dialogs/SettingsDialog.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.4b: Extract SettingsDialog component"

#### 5.4c: Create CameraSelectorDialog
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/dialogs/CameraSelectorDialog.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.4c: Extract CameraSelectorDialog component"

#### 5.4d: Create ResolutionSelectorDialog
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/dialogs/ResolutionSelectorDialog.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.4d: Extract ResolutionSelectorDialog component"

### Step 5.5: Extract Common Components

#### 5.5a: Create MenuItem
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/common/MenuItem.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.5a: Extract MenuItem component"

#### 5.5b: Create SelectableOption
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/common/SelectableOption.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.5b: Extract SelectableOption component"

#### 5.5c: Create StatusBadge
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/common/StatusBadge.kt`

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.5c: Extract StatusBadge component"

### Step 5.6: Delete Old CameraDialogs.kt
```bash
rm app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/components/CameraDialogs.kt
```

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 5.6: Delete old CameraDialogs.kt"

---

## PHASE 6: REORGANIZE SCREEN FILES

### Step 6.1: Move Internal Camera Files
```bash
# Move InternalCameraPreview
mv app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/InternalCameraPreview.kt \
   app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/internal/InternalCameraPreview.kt

# Move and rename ExperimentInternalCameraScreen
mv app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/ExperimentInternalCameraScreen.kt \
   app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/internal/InternalCameraScreen.kt
```

**Changes**:
- Update package declarations
- Update imports in InternalCameraScreen.kt to use extracted components
- Simplify InternalCameraScreen to use new components

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 6.1: Move internal camera files to internal/ subpackage"

### Step 6.2: Move USB Camera Files
```bash
# Move UsbCameraPreview
mv app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/UsbCameraPreview.kt \
   app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/usb/UsbCameraPreview.kt

# Move UsbCameraScreen
mv app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/UsbCameraScreen.kt \
   app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/usb/UsbCameraScreen.kt

# Move UsbCameraFragment
mv app/src/main/java/com/soerjo/myndicam/presentation/fragment/UsbCameraFragment.kt \
   app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/usb/UsbCameraFragment.kt

# Remove empty fragment package
rmdir app/src/main/java/com/soerjo/myndicam/presentation/fragment
```

**Changes**:
- Update package declarations
- Update imports in UsbCameraScreen.kt to use extracted components
- Simplify UsbCameraScreen to use new components

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 6.2: Move USB camera files to usb/ subpackage"

---

## PHASE 7: EXPAND CONSTANTS

### Step 7.1: Update Constants.kt
**File**: `app/src/main/java/com/soerjo/myndicam/core/common/Constants.kt`

**Add**:
```kotlin
object Constants {
    // Existing constants...
    const val TARGET_WIDTH = 1920
    const val TARGET_HEIGHT = 1080
    const val DEFAULT_SOURCE_NAME = "Android Camera"
    const val TARGET_ASPECT_RATIO = 16f / 9f

    // UI Constants
    object Ui {
        // Tally
        const val TALLY_BORDER_WIDTH_DP = 12
        const val TALLY_BLINK_DURATION_MS = 500
        const val COLOR_PROGRAM_TALLY = 0xFF00FF00
        const val COLOR_PREVIEW_TALLY = 0xFFFFFF00

        // Live Badge
        const val LIVE_BADGE_FONT_SIZE_SP = 11
        const val LIVE_BADGE_CORNER_RADIUS_DP = 4
        const val LIVE_BADGE_ALPHA = 0.9f

        // Status Boxes
        const val STATUS_BG_ALPHA_NORMAL = 0.5f
        const val STATUS_BG_ALPHA_OPAQUE = 0.9f
        const val STATUS_CORNER_RADIUS_DP = 8
        const val STATUS_BADGE_CORNER_RADIUS_DP = 4

        // Controls
        const val MAIN_FAB_SIZE_DP = 72
        const val FAB_ICON_SIZE_DP = 36
        const val CONTROL_BUTTON_SIZE_DP = 48
        const val CONTROL_ICON_SIZE_DP = 24
    }
}
```

**Changes**:
- Replace hardcoded values in components with Constants.Ui references
- Update all extracted UI components to use constants

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 7.1: Expand Constants with UI values"

---

## PHASE 8: SIMPLIFY VIEWMODEL

### Step 8.1: Extract CameraUiState.kt to Separate File
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/CameraUiState.kt`

**Move**:
- Extract CameraUiState data class from CameraViewModel.kt
- Move to separate file

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 8.1: Extract CameraUiState to separate file"

### Step 8.2: Final ViewModel Simplification
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/CameraViewModel.kt`

**Changes**:
- Complete refactoring using managers
- Reduce from 550 to ~250 lines
- Delegate NDI operations to NdiManager
- Delegate USB operations to UsbCameraManager
- Keep only UI state management and user interactions

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 8.2: Complete ViewModel simplification"

---

## PHASE 9: SIMPLIFY SCREEN FILES

### Step 9.1: Simplify InternalCameraScreen.kt
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/internal/InternalCameraScreen.kt`

**Changes**:
- Replace inline UI with extracted components
- Use CameraPreviewView, LiveBadge, FpsResolutionInfoBox, etc.
- Remove hardcoded values, use Constants
- Simplify from 792 to ~150 lines

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 9.1: Simplify InternalCameraScreen to use components"

### Step 9.2: Simplify UsbCameraScreen.kt
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/usb/UsbCameraScreen.kt`

**Changes**:
- Replace inline UI with extracted components
- Use overlay, controls, dialogs components
- Remove hardcoded values, use Constants
- Simplify from 512 to ~100 lines

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 9.2: Simplify UsbCameraScreen to use components"

### Step 9.3: Simplify CameraScreen.kt
**File**: `app/src/main/java/com/soerjo/myndicam/presentation/screen/camera/CameraScreen.kt`

**Changes**:
- Simplify routing logic only
- Remove all dialog composables (moved to dialogs/)
- Reduce from 512 to ~50 lines

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 9.3: Simplify CameraScreen to routing only"

---

## PHASE 10: UPDATE DI MODULE

### Step 10.1: Update AppModule.kt
**File**: `app/src/main/java/com/soerjo/myndicam/core/di/AppModule.kt`

**Add**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Existing bindings...

    @Provides
    @Singleton
    fun provideUsbCameraManager(): UsbCameraManager {
        return UsbCameraManager(onFrameCallback = null)
    }
}
```

**Note**: NdiManager is created per ViewModel instance with sourceName, so not bound in module

**Build Check**: `./gradlew assembleDebug`
**Commit**: "Phase 10.1: Update AppModule to inject UsbCameraManager"

---

## PHASE 11: FINAL CLEANUP AND VERIFICATION

### Step 11.1: Update All Imports
**Files to check and update**:
- All screen files (CameraScreen.kt, InternalCameraScreen.kt, UsbCameraScreen.kt)
- All preview files
- CameraViewModel.kt
- Camera controllers

**Action**: Run comprehensive import verification
```bash
./gradlew assembleDebug
```

**Fix**: Any compilation errors from incorrect imports

**Commit**: "Phase 11.1: Update all imports"

### Step 11.2: Final Build Verification
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run lint
./gradlew lint
```

**Expected**: All builds succeed, no errors

### Step 11.3: Code Quality Check
```bash
# Check for any remaining large files
find app/src/main/java -name "*.kt" -exec wc -l {} + | sort -rn | head -10
```

**Success Criteria**:
- No files > 300 lines (except domain models)
- Clean separation of concerns
- All imports resolved
- Build succeeds

### Step 11.4: Final Commit
```bash
git add .
git commit -m "Phase 11.2-11.4: Final restructuring completed successfully

- All large files split and simplified
- Managers extracted from ViewModel
- Conversion logic moved to core/util
- UI components extracted to subpackages
- Constants centralized
- DI module updated
- All builds passing
"
```

### Step 11.5: Verification Summary
Create summary document of changes:
- Files created: 25
- Files moved: 9
- Files modified: 8
- Files deleted: 2
- Lines of code reduced: ~1,859
- Package structure: Deeper nesting achieved

---

## EXECUTION ORDER

1. Create backup branch
2. Phase 1: Create package structure
3. Phase 2: Extract managers
4. Phase 3: Move conversion logic
5. Phase 4: Create CameraHelper
6. Phase 5: Extract UI components
7. Phase 6: Reorganize screen files
8. Phase 7: Expand Constants
9. Phase 8: Simplify ViewModel
10. Phase 9: Simplify screen files
11. Phase 10: Update DI module
12. Phase 11: Final cleanup and verification

**Estimated Total Time**: ~3 hours

---

## SUCCESS CRITERIA

- [ ] All files < 300 lines (except domain models)
- [ ] Clean separation of UI, logic, and data layers
- [ ] No hardcoded values (all in Constants)
- [ ] Deeper package structure with logical grouping
- [ ] All imports updated and correct
- [ ] Project builds successfully (`./gradlew assembleDebug`)
- [ ] No compilation errors
- [ ] No runtime crashes (basic functionality test)

---

## ROLLBACK PLAN

If any phase fails:
```bash
# Revert to backup
git checkout main
git branch -D feature/code-restructuring

# Start from backup branch
git checkout backup-before-restructuring
```
