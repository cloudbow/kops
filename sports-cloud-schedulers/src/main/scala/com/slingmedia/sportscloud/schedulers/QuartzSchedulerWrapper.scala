package com.slingmedia.sportscloud.schedulers

import org.quartz.{ JobBuilder, TriggerBuilder, CronTrigger, CronScheduleBuilder, SchedulerFactory, Scheduler, Job, JobDetail, Trigger, JobExecutionContext }

import java.util.TimeZone

import org.slf4j.LoggerFactory;

object ScheduleType extends Enumeration {
  type ScheduleType = Value
  val CRON_MISFIRE_DO_NOTHING, CRON_MISFIRE_NOW, FIRE_ONCE = Value
}
import ScheduleType._;
import org.quartz.CronTrigger
import org.quartz.Trigger
import org.quartz.TriggerBuilder

case class ScheduledJob(name: String, group: String, jobClass: Class[Any], cronSchedule: String, scheduleType: ScheduleType)

class QuartzSchedulerWrapper {
  private val log = LoggerFactory.getLogger("QuartzSchedulerWrapper")
  private val schedFact: SchedulerFactory = new org.quartz.impl.StdSchedulerFactory()
  private val sched: Scheduler = schedFact.getScheduler()

  private[this] val createJob: (Class[Any], String, String) => JobDetail = (job: Class[Any], name: String, group: String) => {
    val jobDetail: JobDetail = JobBuilder.newJob(job.asInstanceOf[Class[_ <: org.quartz.Job]]).withIdentity(name, group).build();
    jobDetail

  }

  private[this] val createTrigger: (String, String, String, String, String, ScheduleType) => Trigger = (triggerName: String, triggerGroup: String, jobName: String, jobGroup: String, cronScheduleStr: String, scheduleTypeEnum: ScheduleType) => {

    val triggerBuilder: TriggerBuilder[CronTrigger] = TriggerBuilder.newTrigger().withIdentity(triggerName, triggerGroup).forJob(jobName, jobGroup).asInstanceOf[TriggerBuilder[CronTrigger]]
    scheduleTypeEnum match {
      case ScheduleType.CRON_MISFIRE_DO_NOTHING =>
        triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cronScheduleStr).withMisfireHandlingInstructionDoNothing().inTimeZone(TimeZone.getTimeZone("UTC")))
      case ScheduleType.CRON_MISFIRE_NOW =>
        triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cronScheduleStr).withMisfireHandlingInstructionFireAndProceed().inTimeZone(TimeZone.getTimeZone("UTC")))
      case _ =>
      // do nothing
    }
    triggerBuilder.build()
  }

  val scheduleJob: (ScheduledJob) => Unit = (job: ScheduledJob) => {
    log.trace(s"scheduling job of type $job.scheduleType $job")
    job.scheduleType match {
      case ScheduleType.CRON_MISFIRE_DO_NOTHING | CRON_MISFIRE_NOW =>
        val jobDetail = createJob(job.jobClass, job.name, job.group)
        val jobTrigger = createTrigger(job.name.concat("-trigger"), job.group.concat("-trigger"), job.name, job.group, job.cronSchedule, job.scheduleType)
        sched.scheduleJob(jobDetail, jobTrigger)
      case ScheduleType.FIRE_ONCE =>
        val jobDetail = createJob(job.jobClass, job.name, job.group)
        val trigger = TriggerBuilder.newTrigger()
          .startNow()
          .build();
        sched.scheduleJob(jobDetail, trigger)
      case _ =>
      //throw new IllegalArgumentException
    }

  }

  val start: () => Unit = () => {
    sched.start()
  }

  val stop: () => Unit = () => {
    sched.shutdown(true)
  }

  def publishJobs(jobsList: List[ScheduledJob]) = {
    jobsList.foreach { it =>
      scheduleJob(it)
    }
  }

}

object QuartzSchedulerWrapper {
  private val log = LoggerFactory.getLogger("QuartzSchedulerWrapperApp")

  private[this] var quartzScheduler: QuartzSchedulerWrapper = null

  def apply() = {
    log.trace("Inited quartzscheduler wrapper")
    quartzScheduler = new QuartzSchedulerWrapper()
    quartzScheduler.start()
    quartzScheduler
  }

}