#include "filesys/filesys.h"
#include <debug.h>
#include <stdio.h>
#include <string.h>
#include "filesys/file.h"
#include "filesys/free-map.h"
#include "filesys/inode.h"
#include "filesys/directory.h"
#include "filesys/buffer_cache.h"
#include "threads/thread.h"
/* Partition that contains the file system. */
struct block *fs_device;

static void do_format (void);
struct dir* parse_path(char *path_name, char *file_name);
bool filesys_create_dir(const char *name);

bool filesys_create_dir(const char *name)
{
	  char *cp_name = malloc(strlen(name)+1);
	  strlcpy(cp_name,name,strlen(name)+1);
	  char *file_name = malloc(strlen(name)+1);

	  int a,b,c;
	//  printf("cp_name %s\n",cp_name);
	  struct dir *dir = parse_path(cp_name,file_name);  // parse pathname
	  block_sector_t inode_sector = 0;

	  bool success = (dir != NULL
	                   && (a= free_map_allocate (1, &inode_sector))
					   && (b=dir_create (inode_sector, 16)) //directory 16: 최대 엔트리
	                   && (c=dir_add (dir, file_name, inode_sector)));

	  // printf("file create success %d a %d b %d c %d \n",success,a,b,c);

	  if(success){
		  struct dir *new_dir = dir_open(inode_open(inode_sector));
		  dir_add(new_dir,"..",inode_sector);
		  dir_add(new_dir,".",inode_sector);
		  dir_close(new_dir);
	  }
    //  printf("dir_create success %d\n",success);
	  return success;

}

/* Initializes the file system module.
   If FORMAT is true, reformats the file system. */
struct dir* parse_path(char *path_name, char *file_name)
{
	int check = 0 ;
	struct inode *inode;
	struct dir *dir;
	if(path_name == NULL || file_name == NULL)
		return NULL;
	if(strlen(path_name) == 0)
		return NULL;
	char *token, *nextToken, *savePtr;
	
	if(path_name[0] == '/'||!thread_current()->cur_dir) // 절대경로
	{
		dir = dir_open_root();
		if(path_name[1] == '\0')check =1;
	}
	else {                  // 상대경로
		dir = dir_reopen(thread_current()->cur_dir);
	}
	if(token = strtok_r(path_name,"/",&savePtr))
		nextToken = strtok_r(NULL,"/",&savePtr);
	while(token != NULL && nextToken != NULL)
	{
		if(strcmp(token,".") != 0 ){
			if(dir_lookup(dir,token,&inode)); //token and nextToken exist
			else                              // so -> no entry -> fail
			{
				return NULL;
			}

			if(inode_is_dir(inode));          // this file? -> nextToken must be NULL
			else
			{
				inode_close(inode);
				//return NULL;
			}
			dir_close(dir);
			dir = dir_open(inode);  // next directory
		}
		token = nextToken;
		nextToken = strtok_r(NULL,"/",&savePtr);

	}
    //printf("strlen(token name) %d\n",strlen(token));
	if(token != NULL)  // for avoiding error strlcpy
		strlcpy(file_name,token,20);
	else {
		//if(inode_get_inumber(dir_get_inode(dir))==ROOT_DIR_SECTOR
			//	&&  check)
			//strlcpy(file_name,"",1);
		//else
		file_name = NULL;
	}
	//printf("filesys fileName %s\n",file_name);
	return dir;

}


void
filesys_init (bool format) 
{

  fs_device = block_get_role (BLOCK_FILESYS);
  if (fs_device == NULL)
    PANIC ("No file system device found, can't initialize file system.");


  inode_init ();
  bc_init();
  free_map_init ();


  if (format) 
    do_format ();

  free_map_open ();
  thread_current()->cur_dir = dir_open_root();
}

/* Shuts down the file system module, writing any unwritten data
   to disk. */
void
filesys_done (void) 
{
  bc_term();
  free_map_close ();
}

/* Creates a file named NAME with the given INITIAL_SIZE.
   Returns true if successful, false otherwise.
   Fails if a file named NAME already exists,
   or if internal memory allocation fails. */
bool
filesys_create (const char *name, off_t initial_size) 
{
  block_sector_t inode_sector = 0;
  char * cp_name = malloc(strlen(name)+1);
  strlcpy(cp_name,name,strlen(name)+1);
 // printf("cp_name %s\n",cp_name);
  char *file_name = malloc(strlen(name)+1);

  int a,b,c;
  struct dir *dir = parse_path(cp_name,file_name);
 // printf("dir %d filename %s\n",dir,file_name);
  bool success = (dir != NULL
                  && (a= free_map_allocate (1, &inode_sector))
                  && (b=inode_create (inode_sector, initial_size,0)) //file
                  && (c=dir_add (dir, file_name, inode_sector)));

  if (!success && inode_sector != 0) 
    free_map_release (inode_sector, 1);
  dir_close (dir);
  free(cp_name);
 // printf("file create success %d a %d b %d c %d\n",success,a,b,c);
  return success;
}

/* Opens the file with the given NAME.
   Returns the new file if successful or a null pointer
   otherwise.
   Fails if no file named NAME exists,
   or if an internal memory allocation fails. */
struct file *
filesys_open (const char *name)
{
  bool result;
  char *cp_name = malloc(strlen(name)+1);
  strlcpy(cp_name,name,strlen(name)+1);
  char *file_name = malloc(strlen(name)+1);

  struct dir *dir = parse_path(cp_name,file_name);
  // printf("filesys dir %d\n",dir);
   struct inode *inode = NULL;
 // printf("open name %s file_name %s\n",name,file_name);
/*  if (dir != NULL)
    result = dir_lookup (dir, file_name, &inode);
  dir_close (dir);
*/
   if(dir != NULL)
   {
	   if(((inode_get_inumber(dir_get_inode(dir)) == ROOT_DIR_SECTOR)&&
			   	  strlen(file_name)==0 )|| strcmp(file_name,".") == 0)
	   {
		  // free(file_name);
		   return (struct file *)dir;
	   }
	   else{
		   dir_lookup(dir,file_name,&inode);
	   }

   }
   dir_close(dir);
  // free(file_name);
/*
   if(!inode)
   {
	   return NULL;
   }
   if(inode_is_dir(inode))
   {
	   return (struct file *)dir_open(inode);
   }
*/
  return file_open (inode);
}

/* Deletes the file named NAME.
   Returns true if successful, false on failure.
   Fails if no file named NAME exists,
   or if an internal memory allocation fails. */
bool
filesys_remove (const char *name) 
{
  char *cp_name = malloc(strlen(name)+1);
  strlcpy(cp_name,name,strlen(name)+1);
  char *file_name = malloc(strlen(name)+1);


  struct dir *dir = parse_path(cp_name,file_name);

 // if(inode_get_inumber(dir_get_inode(dir)) == ROOT_DIR_SECTOR )
//	  return false;
  bool success = dir != NULL && dir_remove (dir,file_name);
  dir_close (dir); 

  return success;
}

/* Formats the file system. */
static void
do_format (void)
{
  printf ("Formatting file system...");
  free_map_create ();
  //printf("-------do format --------\n");
  if (!dir_create (ROOT_DIR_SECTOR, 16))
    PANIC ("root directory creation failed");
  struct dir *dir= dir_open(inode_open(ROOT_DIR_SECTOR));
  dir_add(dir,"..",ROOT_DIR_SECTOR);          //add directory parent
  dir_add(dir,".",ROOT_DIR_SECTOR);           //add directory self
  dir_close(dir);
  free_map_close ();
  printf ("done.\n");
}
