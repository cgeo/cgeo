/**
 * Downloader
 *
 * Suports downloading files from other servers, including update checking.
 * Primarily used currently for offline maps and its themes as well as for
 * routing data. (See models/Download for available types.)
 *
 * Workflow "selection":
 * - select a map file (or theme file) using the DownloadSelectorActivity
 *   currently available in maps, settings and installation wizard
 * - selected file triggers the "download" workflow
 *
 * Workflow "download":
 * - Android internal Download manager gets triggered for download of a
 *   specific file,
 *   temporary info for this process is stored as PendingDownload
 * - Download manager calls DownloadNotificationReceiver with result
 * - this deletes the PendingDownload and triggers ReceiveDownloadService
 * - here copying the file to its final destination (unzipping along the
 *   way, if needed) and handling of companion file is done
 *
 * Workflow "update":
 * - press "check update" button in DownloadSelectorActivity
 * - checks downloaded files for available updates (using info stored
 *   in companion files)
 * - selection of found files and further workflow as above "selection"
 *
 * Workflow "mf-map-v4" and "mf-theme" schemes:
 * - download can also be triggered in the browser by clicking a link
 *   having one of the above schemes
 * - this will trigger one of the MapDownloadReceiverSchemeXXX activities,
 *   which will check the source for being supported, and triggers the
 *   Android Download manager
 * - further steps as described in the "download" workflow
 *
 * CompanionFiles:
 * - for each downloaded file there will be a companion file in the same
 *   folder having the same file name + addition extension "-cgeo.txt"
 * - This file stores original source uri & filename + date/time
 *   to make this data available for update checking
 */
package cgeo.geocaching.downloader;
