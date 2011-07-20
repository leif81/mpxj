/*
 * file:       ResourceAssignmentFactoryCommon.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2010
 * date:       21/03/2010
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.mpp;

import java.util.List;
import java.util.Set;

import net.sf.mpxj.AssignmentField;
import net.sf.mpxj.Duration;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.ResourceType;
import net.sf.mpxj.SplitTaskFactory;
import net.sf.mpxj.Task;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.TimephasedResourceAssignment;
import net.sf.mpxj.TimephasedResourceAssignmentNormaliser;
import net.sf.mpxj.WorkContour;
import net.sf.mpxj.utility.NumberUtility;
import net.sf.mpxj.utility.RTFUtility;

/**
 * Common implementation detail to extract resource assignment data from 
 * MPP9 and MPP12 files.
 */
public class ResourceAssignmentFactoryCommon
{
   /**
    * Main entry point when called to process assignment data.
    * 
    * @param file parent project file
    * @param fieldMap assignment field map
    * @param useRawTimephasedData use raw timephased data flag
    * @param preserveNoteFormatting preserve note formatting flag
    * @param assnVarMeta var meta
    * @param assnVarData var data
    * @param assnFixedMeta fixed meta
    * @param assnFixedData fixed data
    * @param assnFixedData2 fixed data
    */
   public void process(ProjectFile file, FieldMap fieldMap, boolean useRawTimephasedData, boolean preserveNoteFormatting, VarMeta assnVarMeta, Var2Data assnVarData, FixedMeta assnFixedMeta, FixedData assnFixedData, FixedData assnFixedData2)
   {
      Set<Integer> set = assnVarMeta.getUniqueIdentifierSet();
      int count = assnFixedMeta.getItemCount();
      TimephasedResourceAssignmentFactory timephasedFactory = new TimephasedResourceAssignmentFactory();
      SplitTaskFactory splitFactory = new SplitTaskFactory();
      TimephasedResourceAssignmentNormaliser normaliser = new MPPTimephasedResourceAssignmentNormaliser();
      RTFUtility rtf = new RTFUtility();

      //System.out.println(assnFixedMeta);
      //System.out.println(assnFixedData);
      //System.out.println(assnVarMeta);
      //System.out.println(assnVarData);

      for (int loop = 0; loop < count; loop++)
      {
         byte[] meta = assnFixedMeta.getByteArrayValue(loop);
         if (meta[0] != 0)
         {
            continue;
         }

         int offset = MPPUtility.getInt(meta, 4);
         byte[] data = assnFixedData.getByteArrayValue(assnFixedData.getIndexFromOffset(offset));
         /*
                  if (data == null || data.length <= fieldMap.getMaxFixedDataOffset(0))
                  {
                     continue;
                  }
         */
         if (data == null)
         {
            continue;
         }

         if (data.length <= fieldMap.getMaxFixedDataOffset(0))
         {
            byte[] newData = new byte[fieldMap.getMaxFixedDataOffset(0) + 8];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
         }

         int id = MPPUtility.getInt(data, fieldMap.getFixedDataOffset(AssignmentField.UNIQUE_ID));
         final Integer varDataId = Integer.valueOf(id);
         if (set.contains(varDataId) == false)
         {
            continue;
         }

         byte[] data2 = null;
         if (assnFixedData2 != null)
         {
            data2 = assnFixedData2.getByteArrayValue(loop);
         }

         ResourceAssignment assignment = new ResourceAssignment(file);
         assignment.setTimephasedNormaliser(normaliser);

         assignment.disableEvents();
         fieldMap.populateContainer(assignment, varDataId, new byte[][]
         {
            data,
            data2
         }, assnVarData);
         assignment.enableEvents();

         if (fieldMap.getFieldLocation(AssignmentField.FLAG1) != FieldMap.FieldLocation.VAR_DATA)
         {
            assignment.setFlag(1, (meta[28] & 0x80) != 0);

            assignment.setFlag(2, (meta[29] & 0x01) != 0);
            assignment.setFlag(3, (meta[29] & 0x02) != 0);
            assignment.setFlag(4, (meta[29] & 0x04) != 0);
            assignment.setFlag(5, (meta[29] & 0x08) != 0);
            assignment.setFlag(6, (meta[29] & 0x10) != 0);
            assignment.setFlag(7, (meta[29] & 0x20) != 0);
            assignment.setFlag(8, (meta[29] & 0x40) != 0);
            assignment.setFlag(9, (meta[29] & 0x80) != 0);

            assignment.setFlag(10, (meta[30] & 0x01) != 0);
            assignment.setFlag(11, (meta[30] & 0x02) != 0);
            assignment.setFlag(12, (meta[30] & 0x04) != 0);
            assignment.setFlag(13, (meta[30] & 0x08) != 0);
            assignment.setFlag(14, (meta[30] & 0x10) != 0);
            assignment.setFlag(15, (meta[30] & 0x20) != 0);
            assignment.setFlag(16, (meta[30] & 0x40) != 0);
            assignment.setFlag(17, (meta[30] & 0x80) != 0);

            assignment.setFlag(18, (meta[31] & 0x01) != 0);
            assignment.setFlag(19, (meta[31] & 0x02) != 0);
            assignment.setFlag(20, (meta[31] & 0x04) != 0);
         }

         if (fieldMap.getFieldLocation(AssignmentField.CONFIRMED) != FieldMap.FieldLocation.VAR_DATA)
         {
            assignment.setConfirmed((meta[8] & 0x80) != 0);
         }

         if (fieldMap.getFieldLocation(AssignmentField.RESPONSE_PENDING) != FieldMap.FieldLocation.VAR_DATA)
         {
            assignment.setResponsePending((meta[9] & 0x01) != 0);
         }

         if (fieldMap.getFieldLocation(AssignmentField.TEAM_STATUS_PENDING) != FieldMap.FieldLocation.VAR_DATA)
         {
            assignment.setTeamStatusPending((meta[10] & 0x02) != 0);
         }

         processHyperlinkData(assignment, assnVarData.getByteArray(varDataId, fieldMap.getVarDataKey(AssignmentField.HYPERLINK_DATA)));

         //
         // Post processing
         //
         if (file.getMppFileType() == 9 && assignment.getCreateDate() == null)
         {
            byte[] creationData = assnVarData.getByteArray(varDataId, MPP9_CREATION_DATA);
            if (creationData != null && creationData.length >= 28)
            {
               assignment.setCreateDate(MPPUtility.getTimestamp(creationData, 24));
            }
         }

         String notes = assignment.getNotes();
         if (notes != null)
         {
            if (!preserveNoteFormatting)
            {
               notes = rtf.strip(notes);
            }

            assignment.setNotes(notes);
         }

         Task task = file.getTaskByUniqueID(assignment.getTaskUniqueID());
         if (task != null)
         {
            task.addResourceAssignment(assignment);

            Resource resource = file.getResourceByUniqueID(assignment.getResourceUniqueID());

            ProjectCalendar calendar = null;
            if (resource != null)
            {
               calendar = resource.getResourceCalendar();
            }

            if (calendar == null || task.getIgnoreResourceCalendar())
            {
               calendar = task.getCalendar();
            }

            if (calendar == null)
            {
               calendar = file.getCalendar();
            }

            byte[] completeWork = assnVarData.getByteArray(varDataId, fieldMap.getVarDataKey(AssignmentField.COMPLETE_WORK_DATA));
            byte[] plannedWork = assnVarData.getByteArray(varDataId, fieldMap.getVarDataKey(AssignmentField.PLANNED_WORK_DATA));

            List<TimephasedResourceAssignment> timephasedComplete = timephasedFactory.getCompleteWork(calendar, assignment.getStart(), completeWork);
            List<TimephasedResourceAssignment> timephasedPlanned = timephasedFactory.getPlannedWork(calendar, assignment.getStart(), assignment.getUnits().doubleValue(), plannedWork, timephasedComplete);
            //System.out.println(timephasedComplete);
            //System.out.println(timephasedPlanned);
            assignment.setActualStart(timephasedComplete.isEmpty() ? null : assignment.getStart());
            assignment.setActualFinish((assignment.getRemainingWork().getDuration() == 0 && resource != null) ? assignment.getFinish() : null);

            if (task.getSplits() != null && task.getSplits().isEmpty())
            {
               splitFactory.processSplitData(task, timephasedComplete, timephasedPlanned);
            }

            createTimephasedData(file, assignment, timephasedPlanned, timephasedComplete);

            assignment.setTimephasedPlanned(timephasedPlanned, !useRawTimephasedData);
            assignment.setTimephasedComplete(timephasedComplete, !useRawTimephasedData);

            if (plannedWork != null)
            {
               if (timephasedFactory.getWorkModified(timephasedPlanned))
               {
                  assignment.setWorkContour(WorkContour.CONTOURED);
               }
               else
               {
                  if (plannedWork.length >= 30)
                  {
                     assignment.setWorkContour(WorkContour.getInstance(MPPUtility.getShort(plannedWork, 28)));
                  }
                  else
                  {
                     assignment.setWorkContour(WorkContour.FLAT);
                  }
               }

               //System.out.println(assignment.getWorkContour());
               //System.out.println(assignment);
            }

            file.fireAssignmentReadEvent(assignment);
         }
      }
   }

   /**
    * Extract assignment hyperlink data. 
    * 
    * @param assignment assignment instance
    * @param data hyperlink data
    */
   private void processHyperlinkData(ResourceAssignment assignment, byte[] data)
   {
      if (data != null)
      {
         int offset = 12;

         offset += 12;
         String hyperlink = MPPUtility.getUnicodeString(data, offset);
         offset += ((hyperlink.length() + 1) * 2);

         offset += 12;
         String address = MPPUtility.getUnicodeString(data, offset);
         offset += ((address.length() + 1) * 2);

         offset += 12;
         String subaddress = MPPUtility.getUnicodeString(data, offset);
         offset += ((subaddress.length() + 1) * 2);

         offset += 12;
         String screentip = MPPUtility.getUnicodeString(data, offset);

         assignment.setHyperlink(hyperlink);
         assignment.setHyperlinkAddress(address);
         assignment.setHyperlinkSubAddress(subaddress);
         assignment.setHyperlinkScreenTip(screentip);
      }
   }

   /**
    * Method used to create missing timephased data.
    * 
    * @param file project file
    * @param assignment resource assignment
    * @param timephasedPlanned planned timephased data
    * @param timephasedComplete complete timephased data
    */
   private void createTimephasedData(ProjectFile file, ResourceAssignment assignment, List<TimephasedResourceAssignment> timephasedPlanned, List<TimephasedResourceAssignment> timephasedComplete)
   {
      if (timephasedPlanned.isEmpty() && timephasedComplete.isEmpty())
      {
         Duration workPerDay;

         if (assignment.getResource() == null || assignment.getResource().getType() == ResourceType.WORK)
         {
            workPerDay = TimephasedResourceAssignmentNormaliser.DEFAULT_NORMALIZER_WORK_PER_DAY;
            int units = NumberUtility.getInt(assignment.getUnits());
            if (units != 100)
            {
               workPerDay = Duration.getInstance((workPerDay.getDuration() * units) / 100.0, workPerDay.getUnits());
            }
         }
         else
         {
            if (assignment.getVariableRateUnits() == null)
            {
               Duration workingDays = assignment.getCalendar().getWork(assignment.getStart(), assignment.getFinish(), TimeUnit.DAYS);
               double units = NumberUtility.getDouble(assignment.getUnits());
               double unitsPerDayAsMinutes = (units * 60) / (workingDays.getDuration() * 100);
               workPerDay = Duration.getInstance(unitsPerDayAsMinutes, TimeUnit.MINUTES);
            }
            else
            {
               double unitsPerHour = NumberUtility.getDouble(assignment.getUnits());
               workPerDay = TimephasedResourceAssignmentNormaliser.DEFAULT_NORMALIZER_WORK_PER_DAY;
               Duration hoursPerDay = workPerDay.convertUnits(TimeUnit.HOURS, file.getProjectHeader());
               double unitsPerDayAsHours = (unitsPerHour * hoursPerDay.getDuration()) / 100;
               double unitsPerDayAsMinutes = unitsPerDayAsHours * 60;
               workPerDay = Duration.getInstance(unitsPerDayAsMinutes, TimeUnit.MINUTES);
            }
         }

         TimephasedResourceAssignment tra = new TimephasedResourceAssignment();
         tra.setStart(assignment.getStart());
         tra.setWorkPerDay(workPerDay);
         tra.setModified(false);
         tra.setFinish(assignment.getFinish());
         tra.setTotalWork(assignment.getWork().convertUnits(TimeUnit.MINUTES, file.getProjectHeader()));
         timephasedPlanned.add(tra);
      }
   }

   private static final Integer MPP9_CREATION_DATA = Integer.valueOf(138);
}
