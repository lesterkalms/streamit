#ifndef __CCP_H
#define __CCP_H

#include <netsocket.h>
#include <ccp_session.h>

#include <vector>
#include <map>

using namespace std;

class ccp {

  vector <thread_info*> thread_list;

  int init_nodes;

  int number_of_threads;
  int machines_in_partition;
  int total_work_load;

  timeval cTime;

  map<int, int> partition;         // from thread to machine id

  map<int, unsigned> machines;     // from machine id to ip address

  map<int, int> threadusage;	   // thread to usage

  map<int, int> threadCpuUtil_n;   // thread to cpu utilization, this is normalized when CPU Util =1

  map<int, int> workPerCpu;	

  map<int, float> workToCpuUtil;
  map<int, double> workToCpuUtil_n;

  multimap<int, int> machineTothread;   // machine to thread
  
  map<int, int> threadCpuUtil;	// cpu utilization per thread

  void add_per_cpu_info(int cpuUtil, int cpuIdleTime, unsigned cpuIpAddress);
  
  void find_thread_per_machine_mapping();  // create the multimap

  bool waiting_to_start_execution;
  int initial_iteration;

  vector <ccp_session*> sessions;
  
  int read_config_file(char *fname);
  int read_work_estimate_file(char *file_name);
  void assign_nodes_to_partition();
  void find_cpu_work_estimate();

  void send_cluster_config(int iter = 0);

  void handle_change_in_number_of_nodes();
  void execute_partitioner(int number_of_nodes);

  void cpu_utilization_per_thread();

  thread_info *get_thread_info(int id);
  void find_new_partition(int num_p);
  void find_partition(int num_p, float *targets);

  int partition_distance();

 public:

  ccp(vector <thread_info*> list, int init_n);

  void set_init_iter(int iter);

  int run_ccp();

};


#endif
